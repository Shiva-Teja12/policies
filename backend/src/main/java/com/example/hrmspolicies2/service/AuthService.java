package com.example.hrmspolicies2.service;

import com.example.hrmspolicies2.dto.AuthResponse;
import com.example.hrmspolicies2.dto.ForgotPasswordRequest;
import com.example.hrmspolicies2.dto.LoginRequest;
import com.example.hrmspolicies2.dto.ResetPasswordRequest;
import com.example.hrmspolicies2.dto.SignupRequest;
import com.example.hrmspolicies2.entity.User;
import com.example.hrmspolicies2.enums.AccountStatus;
import com.example.hrmspolicies2.enums.Role;
import com.example.hrmspolicies2.exception.BadRequestException;
import com.example.hrmspolicies2.exception.DuplicateResourceException;
import com.example.hrmspolicies2.exception.ForbiddenException;
import com.example.hrmspolicies2.exception.ResourceNotFoundException;
import com.example.hrmspolicies2.exception.UnauthorizedException;
import com.example.hrmspolicies2.repository.UserRepository;
import com.example.hrmspolicies2.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.hrmspolicies2.entity.PasswordResetToken;
import com.example.hrmspolicies2.repository.PasswordResetTokenRepository;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AuthService {

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
                            + ".{8,72}$"
            );

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final NewJoinerPolicyAssignmentService
            newJoinerPolicyAssignmentService;

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final PasswordResetEmailService passwordResetEmailService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            NewJoinerPolicyAssignmentService newJoinerPolicyAssignmentService,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordResetEmailService passwordResetEmailService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

        if (!request.getPassword()
                .equals(
                        request.getConfirmPassword()
                )) {
            throw new BadRequestException(
                    "Password and confirm password do not match"
            );
        }

        if (!STRONG_PASSWORD
                .matcher(
                        request.getPassword()
                )
                .matches()) {
            throw new BadRequestException(
                    "Password must contain uppercase, "
                            + "lowercase, number and special character"
            );
        }

        if (userRepository
                .existsByEmailIgnoreCase(
                        email
                )) {
            throw new DuplicateResourceException(
                    "An account already exists with this email"
            );
        }

        User employee =
                User.builder()
                        .name(name)
                        .email(email)
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
                "Employee account registered: "
                        + "userId={}, email={}, assignedPolicies={}",
                savedUser.getId(),
                savedUser.getEmail(),
                assignedPolicies
        );

        return AuthResponse.builder()
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
    // The selected portal must match the role stored in MySQL.
    // =========================================================

    @Transactional(readOnly = true)
    public AuthResponse login(
            LoginRequest request
    ) {
        String email =
                normalizeEmail(
                        request.getEmail()
                );

        User user =
                userRepository
                        .findByEmailIgnoreCase(
                                email
                        )
                        .orElseThrow(() ->
                                new UnauthorizedException(
                                        "Invalid email or password"
                                )
                        );

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword()
                );

        if (!passwordMatches) {
            log.warn(
                    "Failed login attempt for email={}",
                    email
            );

            throw new UnauthorizedException(
                    "Invalid email or password"
            );
        }

        if (user.getAccountStatus()
                == AccountStatus.LOCKED) {
            throw new ForbiddenException(
                    "Your account is locked. "
                            + "Please contact support."
            );
        }

        if (user.getAccountStatus()
                == AccountStatus.DISABLED) {
            throw new ForbiddenException(
                    "Your account is disabled. "
                            + "Please contact support."
            );
        }

        if (user.getRole()
                != request.getSelectedRole()) {
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
                "Successful login: userId={}, email={}, role={}",
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

        return AuthResponse.builder()
                .token(token)
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
    // Currently verifies whether an account exists.
    // A production application should send a secure reset token.
    // =========================================================

    @Transactional
    public String forgotPassword(
            ForgotPasswordRequest request
    ) {

        String email =
                normalizeEmail(
                        request.getEmail()
                );

        /*
         * Do not reveal whether the email exists.
         */
        User user =
                userRepository
                        .findByEmailIgnoreCase(email)
                        .orElse(null);

        if (user == null) {
            return "If an account exists for this email, "
                    + "a password reset link has been sent.";
        }

        /*
         * Remove old reset tokens for this user.
         */
        passwordResetTokenRepository
                .deleteByUser(user);

        /*
         * Generate secure random token.
         */
        String token =
                UUID.randomUUID().toString()
                        + UUID.randomUUID();

        PasswordResetToken resetToken =
                new PasswordResetToken();

        resetToken.setToken(token);
        resetToken.setUser(user);

        /*
         * Reset link is valid for 15 minutes.
         */
        resetToken.setExpiresAt(
                LocalDateTime.now()
                        .plusMinutes(15)
        );

        passwordResetTokenRepository.save(
                resetToken
        );

        passwordResetEmailService.sendResetEmail(
                user.getEmail(),
                user.getName(),
                token
        );

        return "If an account exists for this email, "
                + "a password reset link has been sent.";
    }
    // =========================================================
    // RESET PASSWORD
    // The new password is always stored as a BCrypt hash.
    // =========================================================

    @Transactional
    public String resetPassword(
            ResetPasswordRequest request
    ) {

        if (!request.getNewPassword()
                .equals(request.getConfirmPassword())) {

            throw new BadRequestException(
                    "New password and confirm password do not match"
            );
        }

        PasswordResetToken resetToken =
                passwordResetTokenRepository
                        .findByToken(
                                request.getToken()
                        )
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Invalid password reset link"
                                )
                        );

        if (resetToken.isUsed()) {
            throw new BadRequestException(
                    "This password reset link has already been used"
            );
        }

        if (resetToken.isExpired()) {
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

        userRepository.save(user);

        /*
         * Prevent reuse of the reset link.
         */
        resetToken.setUsed(true);

        passwordResetTokenRepository.save(
                resetToken
        );

        /*
         * Only send confirmation.
         * Never send the actual password.
         */
        passwordResetEmailService
                .sendPasswordChangedEmail(
                        user.getEmail(),
                        user.getName()
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