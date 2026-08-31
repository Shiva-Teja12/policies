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

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            NewJoinerPolicyAssignmentService
                    newJoinerPolicyAssignmentService
    ) {
        this.userRepository =
                userRepository;

        this.passwordEncoder =
                passwordEncoder;

        this.jwtService =
                jwtService;

        this.newJoinerPolicyAssignmentService =
                newJoinerPolicyAssignmentService;
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

    @Transactional(readOnly = true)
    public String forgotPassword(
            ForgotPasswordRequest request
    ) {
        if (request.getEmail() == null
                || request.getEmail()
                .isBlank()) {
            throw new BadRequestException(
                    "Email is required"
            );
        }

        String email =
                normalizeEmail(
                        request.getEmail()
                );

        userRepository
                .findByEmailIgnoreCase(
                        email
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No account found with this email"
                        )
                );

        return "Email verified. "
                + "You can now reset your password.";
    }

    // =========================================================
    // RESET PASSWORD
    // The new password is always stored as a BCrypt hash.
    // =========================================================

    @Transactional
    public String resetPassword(
            ResetPasswordRequest request
    ) {
        if (request.getEmail() == null
                || request.getEmail()
                .isBlank()) {
            throw new BadRequestException(
                    "Email is required"
            );
        }

        if (request.getNewPassword() == null
                || request.getNewPassword()
                .length() < 8) {
            throw new BadRequestException(
                    "Password must be at least 8 characters"
            );
        }

        if (request.getConfirmPassword()
                == null) {
            throw new BadRequestException(
                    "Confirm password is required"
            );
        }

        if (!request.getNewPassword()
                .equals(
                        request.getConfirmPassword()
                )) {
            throw new BadRequestException(
                    "Passwords do not match"
            );
        }

        if (!STRONG_PASSWORD
                .matcher(
                        request.getNewPassword()
                )
                .matches()) {
            throw new BadRequestException(
                    "Password must contain uppercase, "
                            + "lowercase, number and special character"
            );
        }

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
                                new ResourceNotFoundException(
                                        "No account found with this email"
                                )
                        );

        user.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        userRepository.save(
                user
        );

        log.info(
                "Password reset completed for userId={}, email={}",
                user.getId(),
                user.getEmail()
        );

        return "Password updated successfully";
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