package com.example.hrmspolicies2.security;

import com.example.hrmspolicies2.entity.User;
import com.example.hrmspolicies2.enums.AccountStatus;
import com.example.hrmspolicies2.enums.Role;
import com.example.hrmspolicies2.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    // =========================================================
    // STRUCTURED LOGGING
    // =========================================================

    private static final Logger log =
            LoggerFactory.getLogger(
                    OAuth2AuthenticationSuccessHandler.class
            );

    private final UserRepository userRepository;
    private final JwtService jwtService;


    public OAuth2AuthenticationSuccessHandler(
            UserRepository userRepository,
            JwtService jwtService
    ) {
        this.userRepository =
                userRepository;

        this.jwtService =
                jwtService;
    }


    // =========================================================
    // GOOGLE OAUTH SUCCESS
    // =========================================================

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        log.info(
                "event=OAUTH2_LOGIN_SUCCESS provider=GOOGLE"
        );

        // =========================================================
        // GET GOOGLE USER DETAILS
        // =========================================================

        OAuth2User oauthUser =
                (OAuth2User) authentication
                        .getPrincipal();

        String email =
                oauthUser.getAttribute(
                        "email"
                );

        String name =
                oauthUser.getAttribute(
                        "name"
                );


        // =========================================================
        // VALIDATE GOOGLE EMAIL
        // =========================================================

        if (email == null
                || email.isBlank()) {

            log.warn(
                    "event=OAUTH2_LOGIN_REJECTED provider=GOOGLE reason=MISSING_EMAIL"
            );

            clearOAuthSession(
                    request
            );

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
                email.trim()
                        .toLowerCase();


        log.debug(
                "event=OAUTH2_USER_RESOLVED provider=GOOGLE email={}",
                normalizedEmail
        );


        // =========================================================
        // FIND EXISTING USER OR CREATE NEW EMPLOYEE
        // =========================================================

        User user =
                userRepository
                        .findByEmailIgnoreCase(
                                normalizedEmail
                        )
                        .map(
                                existingUser -> {

                                    log.info(
                                            "event=OAUTH2_EXISTING_USER_LOGIN userId={} email={} role={}",
                                            existingUser.getId(),
                                            existingUser.getEmail(),
                                            existingUser.getRole()
                                    );

                                    // Update name from Google
                                    // but DO NOT change application role.
                                    if (name != null
                                            && !name.isBlank()) {

                                        existingUser.setName(
                                                name.trim()
                                        );
                                    }

                                    return userRepository.save(
                                            existingUser
                                    );
                                }
                        )
                        .orElseGet(
                                () ->
                                        createEmployee(
                                                name,
                                                normalizedEmail
                                        )
                        );


        // =========================================================
        // CHECK ACCOUNT STATUS
        // =========================================================

        if (user.getAccountStatus()
                != AccountStatus.ACTIVE) {

            log.warn(
                    "event=OAUTH2_LOGIN_REJECTED userId={} email={} reason=ACCOUNT_NOT_ACTIVE accountStatus={}",
                    user.getId(),
                    user.getEmail(),
                    user.getAccountStatus()
            );

            clearOAuthSession(
                    request
            );

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

        /*
         * IMPORTANT:
         * Generate the JWT but never log its value.
         */
        String token =
                jwtService.generateToken(
                        user.getId(),
                        user.getEmail(),
                        user.getRole()
                );


        log.info(
                "event=OAUTH2_APPLICATION_SESSION_CREATED userId={} email={} role={}",
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
                        + encode(
                        token
                )

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


        log.info(
                "event=OAUTH2_REDIRECT_SUCCESS userId={} role={} destination=/oauth-success",
                user.getId(),
                user.getRole()
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
         *
         * IMPORTANT:
         * Never log this random password.
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


        User savedUser =
                userRepository.save(
                        employee
                );


        log.info(
                "event=OAUTH2_USER_CREATED userId={} email={} role={}",
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole()
        );


        return savedUser;
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

            log.debug(
                    "event=OAUTH2_TEMP_SESSION_CLEARED"
            );
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