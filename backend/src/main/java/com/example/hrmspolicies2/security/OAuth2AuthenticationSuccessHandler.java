package com.example.hrmspolicies2.security;

import com.example.hrmspolicies2.entity.User;
import com.example.hrmspolicies2.enums.AccountStatus;
import com.example.hrmspolicies2.enums.Role;
import com.example.hrmspolicies2.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class OAuth2AuthenticationSuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public OAuth2AuthenticationSuccessHandler(
            UserRepository userRepository,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        // =========================================================
        // GET GOOGLE USER DETAILS
        // =========================================================

        OAuth2User oauthUser =
                (OAuth2User) authentication.getPrincipal();

        String email =
                oauthUser.getAttribute("email");

        String name =
                oauthUser.getAttribute("name");

        // =========================================================
        // VALIDATE GOOGLE EMAIL
        // =========================================================

        if (email == null || email.isBlank()) {

            clearOAuthSession(request);

            response.sendRedirect(
                    "http://localhost:3000/login"
                            + "?oauthError="
                            + encode(
                            "Google account did not provide an email address"
                    )
            );

            return;
        }

        String normalizedEmail =
                email.trim().toLowerCase();

        // =========================================================
        // FIND EXISTING USER OR CREATE NEW EMPLOYEE
        // =========================================================

        User user =
                userRepository
                        .findByEmailIgnoreCase(
                                normalizedEmail
                        )
                        .map(existingUser -> {

                            // Update name from Google
                            // but DO NOT change application role.
                            if (
                                    name != null
                                            && !name.isBlank()
                            ) {
                                existingUser.setName(
                                        name.trim()
                                );
                            }

                            return userRepository.save(
                                    existingUser
                            );
                        })
                        .orElseGet(() ->
                                createEmployee(
                                        name,
                                        normalizedEmail
                                )
                        );

        // =========================================================
        // CHECK ACCOUNT STATUS
        // =========================================================

        if (
                user.getAccountStatus()
                        != AccountStatus.ACTIVE
        ) {

            clearOAuthSession(request);

            response.sendRedirect(
                    "http://localhost:3000/login"
                            + "?oauthError="
                            + encode(
                            "Your account is not active"
                    )
            );

            return;
        }

        // =========================================================
        // CREATE APPLICATION JWT
        // =========================================================

        String token =
                jwtService.generateToken(
                        user.getId(),
                        user.getEmail(),
                        user.getRole()
                );

        // =========================================================
        // BUILD FRONTEND REDIRECT URL
        // =========================================================

        String redirectUrl =
                "http://localhost:3000/oauth-success"
                        + "#token="
                        + encode(token)

                        + "&userId="
                        + user.getId()

                        + "&name="
                        + encode(
                        user.getName()
                )

                        + "&email="
                        + encode(
                        user.getEmail()
                )

                        + "&role="
                        + encode(
                        user.getRole()
                                .name()
                )

                        + "&dashboardPath="
                        + encode(
                        user.getRole()
                                .getDashboardPath()
                );

        // =========================================================
        // IMPORTANT
        //
        // Google OAuth temporarily uses an HTTP session.
        //
        // After generating our JWT we remove that session so that
        // protected APIs continue to require the JWT.
        // =========================================================

        clearOAuthSession(
                request
        );

        // =========================================================
        // SEND USER BACK TO NEXT.JS
        // =========================================================

        response.sendRedirect(
                redirectUrl
        );
    }

    // =============================================================
    // CREATE NEW GOOGLE USER
    // =============================================================

    private User createEmployee(
            String googleName,
            String email
    ) {

        String safeName =
                googleName == null
                        || googleName.isBlank()
                        ? email
                        : googleName.trim();

        /*
         * OAuth users do not log in using this password.
         *
         * A random value is stored only because the current
         * users.password database column is NOT NULL.
         */
        String randomPassword =
                "OAUTH_"
                        + UUID.randomUUID();

        User employee =
                User.builder()
                        .name(
                                safeName
                        )
                        .email(
                                email
                        )
                        .password(
                                randomPassword
                        )
                        .role(
                                Role.EMPLOYEE
                        )
                        .accountStatus(
                                AccountStatus.ACTIVE
                        )
                        .build();

        return userRepository.save(
                employee
        );
    }

    // =============================================================
    // CLEAR TEMPORARY GOOGLE OAUTH SESSION
    // =============================================================

    private void clearOAuthSession(
            HttpServletRequest request
    ) {

        /*
         * Removes OAuth authentication information saved
         * by SimpleUrlAuthenticationSuccessHandler.
         */
        clearAuthenticationAttributes(
                request
        );

        /*
         * Remove the current Spring Security Authentication.
         */
        SecurityContextHolder
                .clearContext();

        /*
         * Destroy the temporary HTTP session used during
         * the Google OAuth redirect flow.
         */
        HttpSession session =
                request.getSession(
                        false
                );

        if (session != null) {
            session.invalidate();
        }
    }

    // =============================================================
    // URL ENCODING
    // =============================================================

    private String encode(
            String value
    ) {

        return URLEncoder.encode(
                value == null
                        ? ""
                        : value,
                StandardCharsets.UTF_8
        );
    }
}