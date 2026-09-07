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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private NewJoinerPolicyAssignmentService
            newJoinerPolicyAssignmentService;

    @Mock
    private PasswordResetTokenRepository
            passwordResetTokenRepository;

    @Mock
    private PasswordResetEmailService
            passwordResetEmailService;

    @InjectMocks
    private AuthService authService;

    private SignupRequest signupRequest;
    private LoginRequest loginRequest;


    // =========================================================
    // COMMON TEST DATA
    // =========================================================

    @BeforeEach
    void setUp() {

        signupRequest =
                new SignupRequest();

        signupRequest.setName(
                "Jane Doe"
        );

        signupRequest.setEmail(
                "Jane.Doe@Example.com"
        );

        signupRequest.setPassword(
                "Secure@12"
        );

        signupRequest.setConfirmPassword(
                "Secure@12"
        );


        loginRequest =
                new LoginRequest();

        loginRequest.setEmail(
                "jane.doe@example.com"
        );

        loginRequest.setPassword(
                "Secure@12"
        );

        loginRequest.setSelectedRole(
                Role.EMPLOYEE
        );
    }


    // =========================================================
    // SIGNUP TESTS
    // =========================================================

    @Nested
    @DisplayName("signup()")
    class Signup {


        @Test
        @DisplayName(
                "creates employee account when request is valid"
        )
        void signup_success() {

            when(
                    userRepository
                            .existsByEmailIgnoreCase(
                                    "jane.doe@example.com"
                            )
            ).thenReturn(
                    false
            );


            when(
                    passwordEncoder.encode(
                            "Secure@12"
                    )
            ).thenReturn(
                    "encoded-password"
            );


            User savedUser =
                    User.builder()
                            .id(
                                    1L
                            )
                            .name(
                                    "Jane Doe"
                            )
                            .email(
                                    "jane.doe@example.com"
                            )
                            .password(
                                    "encoded-password"
                            )
                            .role(
                                    Role.EMPLOYEE
                            )
                            .accountStatus(
                                    AccountStatus.ACTIVE
                            )
                            .build();


            when(
                    userRepository.save(
                            any(User.class)
                    )
            ).thenReturn(
                    savedUser
            );


            when(
                    newJoinerPolicyAssignmentService
                            .assignMandatoryPolicies(
                                    savedUser
                            )
            ).thenReturn(
                    2
            );


            AuthResponse response =
                    authService.signup(
                            signupRequest
                    );


            assertThat(
                    response.getMessage()
            ).isEqualTo(
                    "Employee account created successfully"
            );


            assertThat(
                    response.getUserId()
            ).isEqualTo(
                    1L
            );


            assertThat(
                    response.getEmail()
            ).isEqualTo(
                    "jane.doe@example.com"
            );


            assertThat(
                    response.getRole()
            ).isEqualTo(
                    Role.EMPLOYEE
            );


            assertThat(
                    response.getToken()
            ).isNull();


            ArgumentCaptor<User> userCaptor =
                    ArgumentCaptor.forClass(
                            User.class
                    );


            verify(
                    userRepository
            ).save(
                    userCaptor.capture()
            );


            User capturedUser =
                    userCaptor.getValue();


            assertThat(
                    capturedUser.getEmail()
            ).isEqualTo(
                    "jane.doe@example.com"
            );


            assertThat(
                    capturedUser.getPassword()
            ).isEqualTo(
                    "encoded-password"
            );


            assertThat(
                    capturedUser.getRole()
            ).isEqualTo(
                    Role.EMPLOYEE
            );
        }


        @Test
        @DisplayName(
                "throws exception when passwords do not match"
        )
        void signup_passwordMismatch() {

            signupRequest.setConfirmPassword(
                    "Different@1"
            );


            assertThatThrownBy(
                    () ->
                            authService.signup(
                                    signupRequest
                            )
            )
                    .isInstanceOf(
                            BadRequestException.class
                    )
                    .hasMessage(
                            "Password and confirm password do not match"
                    );


            verify(
                    userRepository,
                    never()
            ).save(
                    any(User.class)
            );
        }


        @Test
        @DisplayName(
                "throws exception when password is weak"
        )
        void signup_weakPassword() {

            signupRequest.setPassword(
                    "weak"
            );

            signupRequest.setConfirmPassword(
                    "weak"
            );


            assertThatThrownBy(
                    () ->
                            authService.signup(
                                    signupRequest
                            )
            )
                    .isInstanceOf(
                            BadRequestException.class
                    );


            verify(
                    userRepository,
                    never()
            ).save(
                    any(User.class)
            );
        }


        @Test
        @DisplayName(
                "throws exception when email already exists"
        )
        void signup_duplicateEmail() {

            when(
                    userRepository
                            .existsByEmailIgnoreCase(
                                    "jane.doe@example.com"
                            )
            ).thenReturn(
                    true
            );


            assertThatThrownBy(
                    () ->
                            authService.signup(
                                    signupRequest
                            )
            )
                    .isInstanceOf(
                            DuplicateResourceException.class
                    )
                    .hasMessage(
                            "An account already exists with this email"
                    );


            verify(
                    userRepository,
                    never()
            ).save(
                    any(User.class)
            );
        }
    }


    // =========================================================
    // LOGIN TESTS
    // =========================================================

    @Nested
    @DisplayName("login()")
    class Login {


        @Test
        @DisplayName(
                "returns JWT when credentials and role are valid"
        )
        void login_success() {

            User user =
                    User.builder()
                            .id(
                                    1L
                            )
                            .name(
                                    "Jane Doe"
                            )
                            .email(
                                    "jane.doe@example.com"
                            )
                            .password(
                                    "encoded-password"
                            )
                            .role(
                                    Role.EMPLOYEE
                            )
                            .accountStatus(
                                    AccountStatus.ACTIVE
                            )
                            .build();


            when(
                    userRepository
                            .findByEmailIgnoreCase(
                                    "jane.doe@example.com"
                            )
            ).thenReturn(
                    Optional.of(
                            user
                    )
            );


            when(
                    passwordEncoder.matches(
                            "Secure@12",
                            "encoded-password"
                    )
            ).thenReturn(
                    true
            );


            when(
                    jwtService.generateToken(
                            1L,
                            "jane.doe@example.com",
                            Role.EMPLOYEE
                    )
            ).thenReturn(
                    "jwt-token-value"
            );


            AuthResponse response =
                    authService.login(
                            loginRequest
                    );


            assertThat(
                    response.getToken()
            ).isEqualTo(
                    "jwt-token-value"
            );


            assertThat(
                    response.getMessage()
            ).isEqualTo(
                    "Login successful"
            );


            assertThat(
                    response.getRole()
            ).isEqualTo(
                    Role.EMPLOYEE
            );


            assertThat(
                    response.getEmail()
            ).isEqualTo(
                    "jane.doe@example.com"
            );
        }


        @Test
        @DisplayName(
                "throws UnauthorizedException when user does not exist"
        )
        void login_unknownEmail() {

            when(
                    userRepository
                            .findByEmailIgnoreCase(
                                    "jane.doe@example.com"
                            )
            ).thenReturn(
                    Optional.empty()
            );


            assertThatThrownBy(
                    () ->
                            authService.login(
                                    loginRequest
                            )
            )
                    .isInstanceOf(
                            UnauthorizedException.class
                    )
                    .hasMessage(
                            "Invalid email or password"
                    );


            verify(
                    jwtService,
                    never()
            ).generateToken(
                    any(),
                    anyString(),
                    any()
            );
        }


        @Test
        @DisplayName(
                "throws UnauthorizedException when password is incorrect"
        )
        void login_wrongPassword() {

            User user =
                    User.builder()
                            .id(
                                    1L
                            )
                            .email(
                                    "jane.doe@example.com"
                            )
                            .password(
                                    "encoded-password"
                            )
                            .role(
                                    Role.EMPLOYEE
                            )
                            .accountStatus(
                                    AccountStatus.ACTIVE
                            )
                            .build();


            when(
                    userRepository
                            .findByEmailIgnoreCase(
                                    "jane.doe@example.com"
                            )
            ).thenReturn(
                    Optional.of(
                            user
                    )
            );


            when(
                    passwordEncoder.matches(
                            "Secure@12",
                            "encoded-password"
                    )
            ).thenReturn(
                    false
            );


            assertThatThrownBy(
                    () ->
                            authService.login(
                                    loginRequest
                            )
            )
                    .isInstanceOf(
                            UnauthorizedException.class
                    )
                    .hasMessage(
                            "Invalid email or password"
                    );


            verify(
                    jwtService,
                    never()
            ).generateToken(
                    any(),
                    anyString(),
                    any()
            );
        }


        @Test
        @DisplayName(
                "throws ForbiddenException when account is locked"
        )
        void login_lockedAccount() {

            User user =
                    User.builder()
                            .id(
                                    1L
                            )
                            .email(
                                    "jane.doe@example.com"
                            )
                            .password(
                                    "encoded-password"
                            )
                            .role(
                                    Role.EMPLOYEE
                            )
                            .accountStatus(
                                    AccountStatus.LOCKED
                            )
                            .build();


            when(
                    userRepository
                            .findByEmailIgnoreCase(
                                    "jane.doe@example.com"
                            )
            ).thenReturn(
                    Optional.of(
                            user
                    )
            );


            when(
                    passwordEncoder.matches(
                            "Secure@12",
                            "encoded-password"
                    )
            ).thenReturn(
                    true
            );


            assertThatThrownBy(
                    () ->
                            authService.login(
                                    loginRequest
                            )
            )
                    .isInstanceOf(
                            ForbiddenException.class
                    );
        }


        @Test
        @DisplayName(
                "throws ForbiddenException when account is disabled"
        )
        void login_disabledAccount() {

            User user =
                    User.builder()
                            .id(
                                    1L
                            )
                            .email(
                                    "jane.doe@example.com"
                            )
                            .password(
                                    "encoded-password"
                            )
                            .role(
                                    Role.EMPLOYEE
                            )
                            .accountStatus(
                                    AccountStatus.DISABLED
                            )
                            .build();


            when(
                    userRepository
                            .findByEmailIgnoreCase(
                                    "jane.doe@example.com"
                            )
            ).thenReturn(
                    Optional.of(
                            user
                    )
            );


            when(
                    passwordEncoder.matches(
                            "Secure@12",
                            "encoded-password"
                    )
            ).thenReturn(
                    true
            );


            assertThatThrownBy(
                    () ->
                            authService.login(
                                    loginRequest
                            )
            )
                    .isInstanceOf(
                            ForbiddenException.class
                    );
        }


        @Test
        @DisplayName(
                "throws ForbiddenException when selected role does not match"
        )
        void login_roleMismatch() {

            User user =
                    User.builder()
                            .id(
                                    1L
                            )
                            .email(
                                    "jane.doe@example.com"
                            )
                            .password(
                                    "encoded-password"
                            )
                            .role(
                                    Role.EMPLOYEE
                            )
                            .accountStatus(
                                    AccountStatus.ACTIVE
                            )
                            .build();


            loginRequest.setSelectedRole(
                    Role.HR_ADMIN
            );


            when(
                    userRepository
                            .findByEmailIgnoreCase(
                                    "jane.doe@example.com"
                            )
            ).thenReturn(
                    Optional.of(
                            user
                    )
            );


            when(
                    passwordEncoder.matches(
                            "Secure@12",
                            "encoded-password"
                    )
            ).thenReturn(
                    true
            );


            assertThatThrownBy(
                    () ->
                            authService.login(
                                    loginRequest
                            )
            )
                    .isInstanceOf(
                            ForbiddenException.class
                    );
        }
    }


    // =========================================================
    // FORGOT PASSWORD TESTS
    // =========================================================

    @Nested
    @DisplayName("forgotPassword()")
    class ForgotPassword {


        @Test
        @DisplayName(
                "creates reset token and sends email"
        )
        void forgotPassword_success() {

            ForgotPasswordRequest request =
                    new ForgotPasswordRequest();

            request.setEmail(
                    "jane.doe@example.com"
            );


            User user =
                    User.builder()
                            .id(
                                    1L
                            )
                            .name(
                                    "Jane Doe"
                            )
                            .email(
                                    "jane.doe@example.com"
                            )
                            .build();


            when(
                    userRepository
                            .findByEmailIgnoreCase(
                                    "jane.doe@example.com"
                            )
            ).thenReturn(
                    Optional.of(
                            user
                    )
            );


            String result =
                    authService.forgotPassword(
                            request
                    );


            assertThat(
                    result
            ).isEqualTo(
                    "Reset link has been sent to jane.doe@example.com"
            );


            verify(
                    passwordResetTokenRepository
            ).deleteByUser(
                    user
            );


            verify(
                    passwordResetTokenRepository
            ).save(
                    any(PasswordResetToken.class)
            );


            verify(
                    passwordResetEmailService
            ).sendResetEmail(
                    anyString(),
                    anyString(),
                    anyString()
            );
        }


        @Test
        @DisplayName(
                "throws BadRequestException when email does not exist"
        )
        void forgotPassword_unknownEmail() {

            ForgotPasswordRequest request =
                    new ForgotPasswordRequest();

            request.setEmail(
                    "missing@example.com"
            );


            when(
                    userRepository
                            .findByEmailIgnoreCase(
                                    "missing@example.com"
                            )
            ).thenReturn(
                    Optional.empty()
            );


            assertThatThrownBy(
                    () ->
                            authService.forgotPassword(
                                    request
                            )
            )
                    .isInstanceOf(
                            BadRequestException.class
                    )
                    .hasMessage(
                            "The email is not registered: missing@example.com"
                    );


            verify(
                    passwordResetTokenRepository,
                    never()
            ).save(
                    any(PasswordResetToken.class)
            );
        }
    }


    // =========================================================
    // VALIDATE RESET TOKEN TESTS
    // =========================================================

    @Nested
    @DisplayName("validateResetToken()")
    class ValidateResetToken {


        @Test
        @DisplayName(
                "returns valid message for valid reset token"
        )
        void validateResetToken_success() {

            User user =
                    User.builder()
                            .id(
                                    1L
                            )
                            .email(
                                    "jane.doe@example.com"
                            )
                            .build();


            PasswordResetToken token =
                    new PasswordResetToken();

            token.setToken(
                    "valid-reset-token"
            );

            token.setUser(
                    user
            );

            token.setUsed(
                    false
            );

            token.setExpiresAt(
                    LocalDateTime.now()
                            .plusMinutes(
                                    10
                            )
            );


            when(
                    passwordResetTokenRepository
                            .findByToken(
                                    "valid-reset-token"
                            )
            ).thenReturn(
                    Optional.of(
                            token
                    )
            );


            String result =
                    authService.validateResetToken(
                            "valid-reset-token"
                    );


            assertThat(
                    result
            ).isEqualTo(
                    "Password reset link is valid"
            );
        }


        @Test
        @DisplayName(
                "throws BadRequestException when token is blank"
        )
        void validateResetToken_blankToken() {

            assertThatThrownBy(
                    () ->
                            authService.validateResetToken(
                                    " "
                            )
            )
                    .isInstanceOf(
                            BadRequestException.class
                    )
                    .hasMessage(
                            "Invalid password reset link"
                    );
        }


        @Test
        @DisplayName(
                "throws BadRequestException when token does not exist"
        )
        void validateResetToken_invalidToken() {

            when(
                    passwordResetTokenRepository
                            .findByToken(
                                    "invalid-token"
                            )
            ).thenReturn(
                    Optional.empty()
            );


            assertThatThrownBy(
                    () ->
                            authService.validateResetToken(
                                    "invalid-token"
                            )
            )
                    .isInstanceOf(
                            BadRequestException.class
                    )
                    .hasMessage(
                            "Invalid password reset link"
                    );
        }


        @Test
        @DisplayName(
                "throws BadRequestException when token was already used"
        )
        void validateResetToken_usedToken() {

            User user =
                    User.builder()
                            .id(
                                    1L
                            )
                            .build();


            PasswordResetToken token =
                    new PasswordResetToken();

            token.setUser(
                    user
            );

            token.setUsed(
                    true
            );

            token.setExpiresAt(
                    LocalDateTime.now()
                            .plusMinutes(
                                    10
                            )
            );


            when(
                    passwordResetTokenRepository
                            .findByToken(
                                    "used-token"
                            )
            ).thenReturn(
                    Optional.of(
                            token
                    )
            );


            assertThatThrownBy(
                    () ->
                            authService.validateResetToken(
                                    "used-token"
                            )
            )
                    .isInstanceOf(
                            BadRequestException.class
                    )
                    .hasMessage(
                            "This password reset link has already been used"
                    );
        }
    }


    // =========================================================
    // RESET PASSWORD TESTS
    // =========================================================

    @Nested
    @DisplayName("resetPassword()")
    class ResetPassword {

        private ResetPasswordRequest request;


        @BeforeEach
        void init() {

            request =
                    new ResetPasswordRequest();

            request.setToken(
                    "valid-reset-token"
            );

            request.setNewPassword(
                    "New@Pass1"
            );

            request.setConfirmPassword(
                    "New@Pass1"
            );
        }


        @Test
        @DisplayName(
                "updates password when reset token is valid"
        )
        void resetPassword_success() {

            User user =
                    User.builder()
                            .id(
                                    1L
                            )
                            .name(
                                    "Jane Doe"
                            )
                            .email(
                                    "jane.doe@example.com"
                            )
                            .password(
                                    "old-password"
                            )
                            .build();


            PasswordResetToken resetToken =
                    new PasswordResetToken();

            resetToken.setToken(
                    "valid-reset-token"
            );

            resetToken.setUser(
                    user
            );

            resetToken.setUsed(
                    false
            );

            resetToken.setExpiresAt(
                    LocalDateTime.now()
                            .plusMinutes(
                                    10
                            )
            );


            when(
                    passwordResetTokenRepository
                            .findByToken(
                                    "valid-reset-token"
                            )
            ).thenReturn(
                    Optional.of(
                            resetToken
                    )
            );


            when(
                    passwordEncoder.encode(
                            "New@Pass1"
                    )
            ).thenReturn(
                    "new-encoded-password"
            );


            String result =
                    authService.resetPassword(
                            request
                    );


            assertThat(
                    result
            ).isEqualTo(
                    "Password reset successfully"
            );


            assertThat(
                    user.getPassword()
            ).isEqualTo(
                    "new-encoded-password"
            );


            assertThat(
                    resetToken.isUsed()
            ).isTrue();


            verify(
                    userRepository
            ).save(
                    user
            );


            verify(
                    passwordResetTokenRepository
            ).save(
                    resetToken
            );


            verify(
                    passwordResetEmailService
            ).sendPasswordChangedEmail(
                    "jane.doe@example.com",
                    "Jane Doe"
            );
        }


        @Test
        @DisplayName(
                "throws BadRequestException when passwords do not match"
        )
        void resetPassword_passwordMismatch() {

            request.setConfirmPassword(
                    "Different@1"
            );


            assertThatThrownBy(
                    () ->
                            authService.resetPassword(
                                    request
                            )
            )
                    .isInstanceOf(
                            BadRequestException.class
                    )
                    .hasMessage(
                            "New password and confirm password do not match"
                    );
        }


        @Test
        @DisplayName(
                "throws BadRequestException when new password is weak"
        )
        void resetPassword_weakPassword() {

            request.setNewPassword(
                    "weak"
            );

            request.setConfirmPassword(
                    "weak"
            );


            assertThatThrownBy(
                    () ->
                            authService.resetPassword(
                                    request
                            )
            )
                    .isInstanceOf(
                            BadRequestException.class
                    );
        }


        @Test
        @DisplayName(
                "throws BadRequestException when token is invalid"
        )
        void resetPassword_invalidToken() {

            when(
                    passwordResetTokenRepository
                            .findByToken(
                                    "valid-reset-token"
                            )
            ).thenReturn(
                    Optional.empty()
            );


            assertThatThrownBy(
                    () ->
                            authService.resetPassword(
                                    request
                            )
            )
                    .isInstanceOf(
                            BadRequestException.class
                    )
                    .hasMessage(
                            "Invalid password reset link"
                    );


            verify(
                    userRepository,
                    never()
            ).save(
                    any(User.class)
            );
        }


        @Test
        @DisplayName(
                "throws BadRequestException when token is already used"
        )
        void resetPassword_usedToken() {

            User user =
                    User.builder()
                            .id(
                                    1L
                            )
                            .build();


            PasswordResetToken resetToken =
                    new PasswordResetToken();

            resetToken.setUser(
                    user
            );

            resetToken.setUsed(
                    true
            );

            resetToken.setExpiresAt(
                    LocalDateTime.now()
                            .plusMinutes(
                                    10
                            )
            );


            when(
                    passwordResetTokenRepository
                            .findByToken(
                                    "valid-reset-token"
                            )
            ).thenReturn(
                    Optional.of(
                            resetToken
                    )
            );


            assertThatThrownBy(
                    () ->
                            authService.resetPassword(
                                    request
                            )
            )
                    .isInstanceOf(
                            BadRequestException.class
                    )
                    .hasMessage(
                            "This password reset link has already been used"
                    );
        }
    }
}