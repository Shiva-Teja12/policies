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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService}.
 *
 * The repository, password encoder and JWT service are mocked with
 * Mockito so these tests exercise ONLY the service-layer logic
 * (validation, orchestration, exception mapping) — no Spring context,
 * no database, no HTTP layer. Each public method has both a "happy
 * path" test and one test per negative/edge-case branch.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private SignupRequest signupRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        signupRequest = new SignupRequest();
        signupRequest.setName("Jane Doe");
        signupRequest.setEmail("Jane.Doe@Example.com"); // mixed case on purpose
        signupRequest.setPassword("SecurePass123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("jane.doe@example.com");
        loginRequest.setPassword("SecurePass123");
    }

    // ======================================================
    // SIGNUP
    // ======================================================

    @Nested
    @DisplayName("signup()")
    class Signup {

        @Test
        @DisplayName("creates and returns a new user when input is valid and email is unique")
        void signup_success() {
            when(userRepository.existsByEmail("jane.doe@example.com")).thenReturn(false);
            when(passwordEncoder.encode("SecurePass123")).thenReturn("encoded-pw");

            User savedUser = User.builder()
                    .id(1L)
                    .name("Jane Doe")
                    .email("jane.doe@example.com")
                    .password("encoded-pw")
                    .role("USER")
                    .build();
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            AuthResponse response = authService.signup(signupRequest);

            assertThat(response.getToken()).isNull();
            assertThat(response.getMessage()).isEqualTo("Account created successfully");
            assertThat(response.getRole()).isEqualTo("USER");
            assertThat(response.getEmail()).isEqualTo("jane.doe@example.com");

            // verify the email was normalized (trimmed + lower-cased) before being saved
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getEmail()).isEqualTo("jane.doe@example.com");
            assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-pw");
        }

        @Test
        @DisplayName("throws BadRequestException when name is blank")
        void signup_blankName_throwsBadRequest() {
            signupRequest.setName("   ");

            assertThatThrownBy(() -> authService.signup(signupRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Name is required");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws BadRequestException when name is null")
        void signup_nullName_throwsBadRequest() {
            signupRequest.setName(null);

            assertThatThrownBy(() -> authService.signup(signupRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Name is required");
        }

        @Test
        @DisplayName("throws BadRequestException when email is blank")
        void signup_blankEmail_throwsBadRequest() {
            signupRequest.setEmail("  ");

            assertThatThrownBy(() -> authService.signup(signupRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Email is required");
        }

        @Test
        @DisplayName("throws BadRequestException when password is shorter than 6 characters")
        void signup_shortPassword_throwsBadRequest() {
            signupRequest.setPassword("123");

            assertThatThrownBy(() -> authService.signup(signupRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Password must be at least 6 characters");
        }

        @Test
        @DisplayName("throws BadRequestException when password is null")
        void signup_nullPassword_throwsBadRequest() {
            signupRequest.setPassword(null);

            assertThatThrownBy(() -> authService.signup(signupRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Password must be at least 6 characters");
        }

        @Test
        @DisplayName("throws DuplicateResourceException when the email is already registered")
        void signup_duplicateEmail_throwsDuplicateResource() {
            when(userRepository.existsByEmail("jane.doe@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.signup(signupRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessage("Email already registered");

            verify(userRepository, never()).save(any());
            verify(passwordEncoder, never()).encode(anyString());
        }
    }

    // ======================================================
    // LOGIN
    // ======================================================

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("returns a token and user details on valid credentials")
        void login_success() {
            User user = User.builder()
                    .id(1L)
                    .name("Jane Doe")
                    .email("jane.doe@example.com")
                    .password("encoded-pw")
                    .role("USER")
                    .build();

            when(userRepository.findByEmail("jane.doe@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("SecurePass123", "encoded-pw")).thenReturn(true);
            when(jwtService.generateToken("jane.doe@example.com", "USER")).thenReturn("jwt-token-value");

            AuthResponse response = authService.login(loginRequest);

            assertThat(response.getToken()).isEqualTo("jwt-token-value");
            assertThat(response.getMessage()).isEqualTo("Login successful");
            assertThat(response.getRole()).isEqualTo("USER");
            assertThat(response.getEmail()).isEqualTo("jane.doe@example.com");
        }

        @Test
        @DisplayName("throws BadRequestException when email is null")
        void login_nullEmail_throwsBadRequest() {
            loginRequest.setEmail(null);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Email and password are required");

            verify(userRepository, never()).findByEmail(any());
        }

        @Test
        @DisplayName("throws BadRequestException when password is null")
        void login_nullPassword_throwsBadRequest() {
            loginRequest.setPassword(null);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Email and password are required");
        }

        @Test
        @DisplayName("throws UnauthorizedException when no account exists for the email")
        void login_unknownEmail_throwsUnauthorized() {
            when(userRepository.findByEmail("jane.doe@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid email or password");

            verify(jwtService, never()).generateToken(any(), any());
        }

        @Test
        @DisplayName("throws UnauthorizedException when the password does not match")
        void login_wrongPassword_throwsUnauthorized() {
            User user = User.builder()
                    .email("jane.doe@example.com")
                    .password("encoded-pw")
                    .role("USER")
                    .build();

            when(userRepository.findByEmail("jane.doe@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("SecurePass123", "encoded-pw")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid email or password");

            verify(jwtService, never()).generateToken(any(), any());
        }
    }

    // ======================================================
    // FORGOT PASSWORD
    // ======================================================

    @Nested
    @DisplayName("forgotPassword()")
    class Forgot {

        @Test
        @DisplayName("returns a confirmation message when the email is registered")
        void forgotPassword_success() {
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("jane.doe@example.com");

            when(userRepository.findByEmail("jane.doe@example.com"))
                    .thenReturn(Optional.of(User.builder().email("jane.doe@example.com").build()));

            String result = authService.forgotPassword(request);

            assertThat(result).isEqualTo("Email verified. You can now reset your password.");
        }

        @Test
        @DisplayName("throws BadRequestException when email is blank")
        void forgotPassword_blankEmail_throwsBadRequest() {
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail(" ");

            assertThatThrownBy(() -> authService.forgotPassword(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Email is required");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when no account exists for the email")
        void forgotPassword_unknownEmail_throwsResourceNotFound() {
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("missing@example.com");

            when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.forgotPassword(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("No account found with this email");
        }
    }

    // ======================================================
    // RESET PASSWORD
    // ======================================================

    @Nested
    @DisplayName("resetPassword()")
    class Reset {

        private ResetPasswordRequest request;

        @BeforeEach
        void init() {
            request = new ResetPasswordRequest();
            request.setEmail("jane.doe@example.com");
            request.setNewPassword("NewSecurePass456");
            request.setConfirmPassword("NewSecurePass456");
        }

        @Test
        @DisplayName("encodes and saves the new password when everything is valid")
        void resetPassword_success() {
            User user = User.builder().email("jane.doe@example.com").password("old-encoded").build();

            when(userRepository.findByEmail("jane.doe@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("NewSecurePass456")).thenReturn("new-encoded");

            String result = authService.resetPassword(request);

            assertThat(result).isEqualTo("Password updated successfully");
            assertThat(user.getPassword()).isEqualTo("new-encoded");
            verify(userRepository, times(1)).save(user);
        }

        @Test
        @DisplayName("throws BadRequestException when email is blank")
        void resetPassword_blankEmail_throwsBadRequest() {
            request.setEmail("");

            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Email is required");
        }

        @Test
        @DisplayName("throws BadRequestException when new password is too short")
        void resetPassword_shortPassword_throwsBadRequest() {
            request.setNewPassword("123");

            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Password must be at least 6 characters");
        }

        @Test
        @DisplayName("throws BadRequestException when confirmPassword is null")
        void resetPassword_nullConfirmPassword_throwsBadRequest() {
            request.setConfirmPassword(null);

            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Confirm password is required");
        }

        @Test
        @DisplayName("throws BadRequestException when passwords do not match")
        void resetPassword_mismatchedPasswords_throwsBadRequest() {
            request.setConfirmPassword("SomethingElse789");

            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Passwords do not match");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when no account exists for the email")
        void resetPassword_unknownEmail_throwsResourceNotFound() {
            when(userRepository.findByEmail("jane.doe@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("No account found with this email");

            verify(passwordEncoder, never()).encode(anyString());
        }
    }
}