package com.example.hrmspolicies2.service;

import com.example.hrmspolicies2.dto.AuthResponse;
import com.example.hrmspolicies2.dto.ForgotPasswordRequest;
import com.example.hrmspolicies2.dto.LoginRequest;
import com.example.hrmspolicies2.dto.ResetPasswordRequest;
import com.example.hrmspolicies2.dto.SignupRequest;
import com.example.hrmspolicies2.entity.PasswordResetToken;
import com.example.hrmspolicies2.entity.User;
import com.example.hrmspolicies2.enums.AccountStatus;
import com.example.hrmspolicies2.enums.Role;
import com.example.hrmspolicies2.exception.BadRequestException;
import com.example.hrmspolicies2.exception.DuplicateResourceException;
import com.example.hrmspolicies2.exception.ForbiddenException;
import com.example.hrmspolicies2.exception.UnauthorizedException;
import com.example.hrmspolicies2.repository.PasswordResetTokenRepository;
import com.example.hrmspolicies2.repository.UserRepository;
import com.example.hrmspolicies2.security.JwtService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthService {

    // =========================================================
    // STRUCTURED LOGGING
    // =========================================================

    private static final Logger log =
            LoggerFactory.getLogger(
                    AuthService.class
            );

    private static final Pattern STRONG_PASSWORD =
            Pattern.compile(
                    "^(?=.*[a-z])"
                            + "(?=.*[A-Z])"
                            + "(?=.*\\d)"
                            + "(?=.*[^A-Za-z\\d])"
                            + ".{8,12}$"
            );

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final NewJoinerPolicyAssignmentService
            newJoinerPolicyAssignmentService;

    private final PasswordResetTokenRepository
            passwordResetTokenRepository;

    private final PasswordResetEmailService
            passwordResetEmailService;


    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            NewJoinerPolicyAssignmentService
                    newJoinerPolicyAssignmentService,
            PasswordResetTokenRepository
                    passwordResetTokenRepository,
            PasswordResetEmailService
                    passwordResetEmailService
    ) {
        this.userRepository =
                userRepository;

        this.passwordEncoder =
                passwordEncoder;

        this.jwtService =
                jwtService;

        this.newJoinerPolicyAssignmentService =
                newJoinerPolicyAssignmentService;

        this.passwordResetTokenRepository =
                passwordResetTokenRepository;

        this.passwordResetEmailService =
                passwordResetEmailService;
    }


    // =========================================================
    // EMPLOYEE SIGNUP
    // Public signup always creates an EMPLOYEE account.
    // The frontend cannot assign privileged roles.
    // =========================================================

    @Transactional
    public AuthResponse signup(
            SignupRequest request
    ) {

        String name =
                request.getName()
                        .trim();

        String email =
                normalizeEmail(
                        request.getEmail()
                );

        log.info(
                "event=USER_SIGNUP_REQUEST email={} requestedRole={}",
                email,
                Role.EMPLOYEE
        );

        if (!request.getPassword()
                .equals(
                        request.getConfirmPassword()
                )) {

            log.warn(
                    "event=USER_SIGNUP_REJECTED email={} reason=PASSWORD_MISMATCH",
                    email
            );

            throw new BadRequestException(
                    "Password and confirm password do not match"
            );
        }

        if (!STRONG_PASSWORD
                .matcher(
                        request.getPassword()
                )
                .matches()) {

            log.warn(
                    "event=USER_SIGNUP_REJECTED email={} reason=WEAK_PASSWORD",
                    email
            );

            throw new BadRequestException(
                    "Password must be 8 to 12 characters and contain "
                            + "at least one uppercase letter, lowercase letter, "
                            + "number and special character"
            );
        }

        if (userRepository
                .existsByEmailIgnoreCase(
                        email
                )) {

            log.warn(
                    "event=USER_SIGNUP_REJECTED email={} reason=DUPLICATE_EMAIL",
                    email
            );

            throw new DuplicateResourceException(
                    "An account already exists with this email"
            );
        }

        User employee =
                User.builder()
                        .name(
                                name
                        )
                        .email(
                                email
                        )
                        .password(
                                passwordEncoder
                                        .encode(
                                                request.getPassword()
                                        )
                        )
                        .role(
                                Role.EMPLOYEE
                        )
                        .accountStatus(
                                AccountStatus.ACTIVE
                        )
                        .build();

        User savedUser =
                userRepository.save(
                        employee
                );

        /*
         * Assign all currently published mandatory policies
         * that are applicable to the new Employee.
         *
         * Signup contains only name, email and password.
         * Therefore, ALL policies can be assigned immediately.
         * Department/grade policies are assigned later when
         * HR updates the Employee profile.
         */
        int assignedPolicies =
                newJoinerPolicyAssignmentService
                        .assignMandatoryPolicies(
                                savedUser
                        );

        log.info(
                "event=USER_SIGNUP_SUCCESS userId={} email={} role={} assignedPolicies={}",
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole(),
                assignedPolicies
        );

        return AuthResponse
                .builder()
                .message(
                        "Employee account created successfully"
                )
                .userId(
                        savedUser.getId()
                )
                .name(
                        savedUser.getName()
                )
                .email(
                        savedUser.getEmail()
                )
                .role(
                        savedUser.getRole()
                )
                .dashboardPath(
                        savedUser.getRole()
                                .getDashboardPath()
                )
                .build();
    }


    // =========================================================
    // ROLE-VERIFIED LOGIN
    // The selected portal must match the role stored in DB.
    // =========================================================

    @Transactional(readOnly = true)
    public AuthResponse login(
            LoginRequest request
    ) {

        String email =
                normalizeEmail(
                        request.getEmail()
                );

        log.info(
                "event=LOGIN_REQUEST email={} selectedRole={}",
                email,
                request.getSelectedRole()
        );

        User user =
                userRepository
                        .findByEmailIgnoreCase(
                                email
                        )
                        .orElseThrow(
                                () -> {

                                    log.warn(
                                            "event=LOGIN_FAILED email={} reason=USER_NOT_FOUND",
                                            email
                                    );

                                    return new UnauthorizedException(
                                            "Invalid email or password"
                                    );
                                }
                        );

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword()
                );

        if (!passwordMatches) {

            log.warn(
                    "event=LOGIN_FAILED email={} reason=INVALID_PASSWORD",
                    email
            );

            throw new UnauthorizedException(
                    "Invalid email or password"
            );
        }

        if (user.getAccountStatus()
                == AccountStatus.LOCKED) {

            log.warn(
                    "event=LOGIN_REJECTED userId={} email={} reason=ACCOUNT_LOCKED",
                    user.getId(),
                    email
            );

            throw new ForbiddenException(
                    "Your account is locked. "
                            + "Please contact support."
            );
        }

        if (user.getAccountStatus()
                == AccountStatus.DISABLED) {

            log.warn(
                    "event=LOGIN_REJECTED userId={} email={} reason=ACCOUNT_DISABLED",
                    user.getId(),
                    email
            );

            throw new ForbiddenException(
                    "Your account is disabled. "
                            + "Please contact support."
            );
        }

        if (user.getRole()
                != request.getSelectedRole()) {

            log.warn(
                    "event=LOGIN_REJECTED userId={} email={} reason=ROLE_MISMATCH actualRole={} selectedRole={}",
                    user.getId(),
                    email,
                    user.getRole(),
                    request.getSelectedRole()
            );

            throw new ForbiddenException(
                    "This account is not authorized for the selected "
                            + portalName(
                            request.getSelectedRole()
                    )
                            + " portal"
            );
        }

        String token =
                jwtService.generateToken(
                        user.getId(),
                        user.getEmail(),
                        user.getRole()
                );

        log.info(
                "event=LOGIN_SUCCESS userId={} email={} role={}",
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

        return AuthResponse
                .builder()
                .token(
                        token
                )
                .message(
                        "Login successful"
                )
                .userId(
                        user.getId()
                )
                .name(
                        user.getName()
                )
                .email(
                        user.getEmail()
                )
                .role(
                        user.getRole()
                )
                .dashboardPath(
                        user.getRole()
                                .getDashboardPath()
                )
                .build();
    }


    // =========================================================
    // FORGOT PASSWORD
    // =========================================================

    @Transactional
    public String forgotPassword(
            ForgotPasswordRequest request
    ) {

        String email =
                normalizeEmail(
                        request.getEmail()
                );

        log.info(
                "event=PASSWORD_RESET_REQUEST email={}",
                email
        );

        User user =
                userRepository
                        .findByEmailIgnoreCase(
                                email
                        )
                        .orElse(
                                null
                        );

        if (user == null) {

            log.warn(
                    "event=PASSWORD_RESET_REQUEST_FAILED email={} reason=USER_NOT_FOUND",
                    email
            );

            throw new BadRequestException(
                    "The email is not registered: "
                            + email
            );
        }

        /*
         * Remove previous reset tokens
         * belonging to this user.
         */
        passwordResetTokenRepository
                .deleteByUser(
                        user
                );

        log.debug(
                "event=PASSWORD_RESET_OLD_TOKENS_REMOVED userId={}",
                user.getId()
        );

        /*
         * Generate secure random token.
         *
         * IMPORTANT:
         * Never log this token.
         */
        String token =
                UUID.randomUUID()
                        .toString()
                        + UUID.randomUUID();

        PasswordResetToken resetToken =
                new PasswordResetToken();

        resetToken.setToken(
                token
        );

        resetToken.setUser(
                user
        );

        /*
         * Reset link remains valid according
         * to the existing application logic.
         */
        resetToken.setExpiresAt(
                LocalDateTime.now()
                        .plusMinutes(
                                10
                        )
        );

        passwordResetTokenRepository.save(
                resetToken
        );

        log.debug(
                "event=PASSWORD_RESET_TOKEN_CREATED userId={} expiresAt={}",
                user.getId(),
                resetToken.getExpiresAt()
        );

        passwordResetEmailService
                .sendResetEmail(
                        user.getEmail(),
                        user.getName(),
                        token
                );

        log.info(
                "event=PASSWORD_RESET_EMAIL_SENT userId={} email={}",
                user.getId(),
                user.getEmail()
        );

        return "Reset link has been sent to "
                + email;
    }


    // =========================================================
    // VALIDATE PASSWORD RESET TOKEN
    // Called immediately when /reset-password opens.
    // =========================================================

    @Transactional(readOnly = true)
    public String validateResetToken(
            String token
    ) {

        log.debug(
                "event=PASSWORD_RESET_TOKEN_VALIDATION_REQUEST"
        );

        if (token == null
                || token.isBlank()) {

            log.warn(
                    "event=PASSWORD_RESET_TOKEN_VALIDATION_FAILED reason=MISSING_TOKEN"
            );

            throw new BadRequestException(
                    "Invalid password reset link"
            );
        }

        PasswordResetToken resetToken =
                passwordResetTokenRepository
                        .findByToken(
                                token
                        )
                        .orElseThrow(
                                () -> {

                                    log.warn(
                                            "event=PASSWORD_RESET_TOKEN_VALIDATION_FAILED reason=TOKEN_NOT_FOUND"
                                    );

                                    return new BadRequestException(
                                            "Invalid password reset link"
                                    );
                                }
                        );

        /*
         * Do not allow the same reset link
         * to be reused.
         */
        if (resetToken.isUsed()) {

            log.warn(
                    "event=PASSWORD_RESET_TOKEN_VALIDATION_FAILED userId={} reason=TOKEN_ALREADY_USED",
                    resetToken.getUser()
                            .getId()
            );

            throw new BadRequestException(
                    "This password reset link has already been used"
            );
        }

        /*
         * Check expiry immediately when
         * the reset-password page opens.
         */
        if (resetToken.isExpired()) {

            log.warn(
                    "event=PASSWORD_RESET_TOKEN_VALIDATION_FAILED userId={} reason=TOKEN_EXPIRED",
                    resetToken.getUser()
                            .getId()
            );

            throw new BadRequestException(
                    "Password reset link has expired"
            );
        }

        log.debug(
                "event=PASSWORD_RESET_TOKEN_VALIDATION_SUCCESS userId={}",
                resetToken.getUser()
                        .getId()
        );

        return "Password reset link is valid";
    }


    // =========================================================
    // RESET PASSWORD
    // The new password is stored as a BCrypt hash.
    // =========================================================

    @Transactional
    public String resetPassword(
            ResetPasswordRequest request
    ) {

        log.info(
                "event=PASSWORD_RESET_SUBMIT_REQUEST"
        );

        if (!request.getNewPassword()
                .equals(
                        request.getConfirmPassword()
                )) {

            log.warn(
                    "event=PASSWORD_RESET_FAILED reason=PASSWORD_MISMATCH"
            );

            throw new BadRequestException(
                    "New password and confirm password do not match"
            );
        }

        if (!STRONG_PASSWORD
                .matcher(
                        request.getNewPassword()
                )
                .matches()) {

            log.warn(
                    "event=PASSWORD_RESET_FAILED reason=WEAK_PASSWORD"
            );

            throw new BadRequestException(
                    "Password must be 8 to 12 characters and contain "
                            + "at least one uppercase letter, lowercase letter, "
                            + "number and special character"
            );
        }

        PasswordResetToken resetToken =
                passwordResetTokenRepository
                        .findByToken(
                                request.getToken()
                        )
                        .orElseThrow(
                                () -> {

                                    log.warn(
                                            "event=PASSWORD_RESET_FAILED reason=INVALID_RESET_TOKEN"
                                    );

                                    return new BadRequestException(
                                            "Invalid password reset link"
                                    );
                                }
                        );

        /*
         * Backend checks are still required.
         * Do not rely only on frontend validation.
         */
        if (resetToken.isUsed()) {

            log.warn(
                    "event=PASSWORD_RESET_FAILED userId={} reason=TOKEN_ALREADY_USED",
                    resetToken.getUser()
                            .getId()
            );

            throw new BadRequestException(
                    "This password reset link has already been used"
            );
        }

        if (resetToken.isExpired()) {

            log.warn(
                    "event=PASSWORD_RESET_FAILED userId={} reason=TOKEN_EXPIRED",
                    resetToken.getUser()
                            .getId()
            );

            throw new BadRequestException(
                    "Password reset link has expired"
            );
        }

        User user =
                resetToken.getUser();

        /*
         * Never save plain-text passwords.
         */
        user.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        userRepository.save(
                user
        );

        /*
         * Mark the token as used so the
         * reset link cannot be reused.
         */
        resetToken.setUsed(
                true
        );

        passwordResetTokenRepository.save(
                resetToken
        );

        /*
         * Send password-change confirmation.
         * Never send the actual password.
         */
        passwordResetEmailService
                .sendPasswordChangedEmail(
                        user.getEmail(),
                        user.getName()
                );

        log.info(
                "event=PASSWORD_RESET_SUCCESS userId={} email={}",
                user.getId(),
                user.getEmail()
        );

        return "Password reset successfully";
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private String normalizeEmail(
            String email
    ) {

        return email.trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }


    private String portalName(
            Role role
    ) {

        return switch (role) {

            case HR_ADMIN ->
                    "HR Admin";

            case LEGAL_REVIEWER ->
                    "Legal Reviewer";

            case HR_HEAD ->
                    "HR Head";

            case MANAGING_DIRECTOR ->
                    "Managing Director";

            case MANAGER ->
                    "Manager";

            case EMPLOYEE ->
                    "Employee";
        };
    }
}