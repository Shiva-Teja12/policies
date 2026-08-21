package com.example.hrmspolicies2.service;

import com.example.hrmspolicies2.dto.AuthResponse;
import com.example.hrmspolicies2.dto.ForgotPasswordRequest;
import com.example.hrmspolicies2.dto.LoginRequest;
import com.example.hrmspolicies2.dto.ResetPasswordRequest;
import com.example.hrmspolicies2.dto.SignupRequest;
import com.example.hrmspolicies2.entity.User;
import com.example.hrmspolicies2.exception.BadRequestException;
import com.example.hrmspolicies2.exception.DuplicateResourceException;
import com.example.hrmspolicies2.exception.ResourceNotFoundException;
import com.example.hrmspolicies2.exception.UnauthorizedException;
import com.example.hrmspolicies2.repository.UserRepository;
import com.example.hrmspolicies2.security.JwtService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // ==========================================
    // SIGNUP
    // ==========================================

    public AuthResponse signup(
            SignupRequest request
    ) {

        if (request.getName() == null ||
                request.getName().trim().isEmpty()) {

            throw new BadRequestException(
                    "Name is required"
            );
        }

        if (request.getEmail() == null ||
                request.getEmail().trim().isEmpty()) {

            throw new BadRequestException(
                    "Email is required"
            );
        }

        if (request.getPassword() == null ||
                request.getPassword().length() < 6) {

            throw new BadRequestException(
                    "Password must be at least 6 characters"
            );
        }

        String name =
                request.getName().trim();

        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase();

        String password =
                request.getPassword();

        if (userRepository.existsByEmail(email)) {

            throw new DuplicateResourceException(
                    "Email already registered"
            );
        }

        User user =
                User.builder()
                        .name(name)
                        .email(email)
                        .password(
                                passwordEncoder.encode(
                                        password
                                )
                        )
                        .role("USER")
                        .build();

        User savedUser =
                userRepository.save(user);

        // Never log the raw or encoded password - only non-sensitive identifiers.
        log.info("New user registered: email={}", savedUser.getEmail());

        return new AuthResponse(
                null,
                "Account created successfully",
                savedUser.getRole(),
                savedUser.getEmail()
        );
    }

    // ==========================================
    // LOGIN
    // ==========================================

    public AuthResponse login(
            LoginRequest request
    ) {

        if (request.getEmail() == null ||
                request.getPassword() == null) {

            throw new BadRequestException(
                    "Email and password are required"
            );
        }

        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase();

        User user =
                userRepository
                        .findByEmail(email)
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

            log.warn("Failed login attempt for email={}", email);

            throw new UnauthorizedException(
                    "Invalid email or password"
            );
        }

        String token =
                jwtService.generateToken(
                        user.getEmail(),
                        user.getRole()
                );

        log.info("User logged in: email={}", email);

        return new AuthResponse(
                token,
                "Login successful",
                user.getRole(),
                user.getEmail()
        );
    }

    // ==========================================
    // FORGOT PASSWORD
    // ==========================================

    public String forgotPassword(
            ForgotPasswordRequest request
    ) {

        if (request.getEmail() == null ||
                request.getEmail().trim().isEmpty()) {

            throw new BadRequestException(
                    "Email is required"
            );
        }

        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase();

        /*
         * This endpoint verifies that the
         * email is registered.
         *
         * The actual password change happens
         * in resetPassword().
         */

        userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No account found with this email"
                        )
                );

        return "Email verified. You can now reset your password.";
    }

    // ==========================================
    // RESET PASSWORD
    // ==========================================

    public String resetPassword(
            ResetPasswordRequest request
    ) {

        if (request.getEmail() == null ||
                request.getEmail().trim().isEmpty()) {

            throw new BadRequestException(
                    "Email is required"
            );
        }

        if (request.getNewPassword() == null ||
                request.getNewPassword().length() < 6) {

            throw new BadRequestException(
                    "Password must be at least 6 characters"
            );
        }

        if (request.getConfirmPassword() == null) {

            throw new BadRequestException(
                    "Confirm password is required"
            );
        }

        if (!request.getNewPassword()
                .equals(request.getConfirmPassword())) {

            throw new BadRequestException(
                    "Passwords do not match"
            );
        }

        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase();

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "No account found with this email"
                                )
                        );

        /*
         * IMPORTANT:
         *
         * The new password is encrypted using BCrypt
         * before it is stored in PostgreSQL.
         */

        String encodedPassword =
                passwordEncoder.encode(
                        request.getNewPassword()
                );

        user.setPassword(encodedPassword);

        userRepository.save(user);

        log.info("Password reset completed for email={}", email);

        return "Password updated successfully";
    }
}