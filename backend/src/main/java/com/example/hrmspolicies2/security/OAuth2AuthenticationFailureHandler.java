package com.example.hrmspolicies2.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2AuthenticationFailureHandler
        implements AuthenticationFailureHandler {

    // =========================================================
    // STRUCTURED LOGGING
    // =========================================================

    private static final Logger log =
            LoggerFactory.getLogger(
                    OAuth2AuthenticationFailureHandler.class
            );


    // =========================================================
    // GOOGLE OAUTH FAILURE
    // =========================================================

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        String message =
                exception.getMessage() == null
                        ? "Google authentication failed"
                        : exception.getMessage();


        log.warn(
                "event=OAUTH2_LOGIN_FAILED provider=GOOGLE path={} exceptionType={}",
                request.getRequestURI(),
                exception.getClass()
                        .getSimpleName()
        );


        /*
         * IMPORTANT:
         * We do NOT log OAuth tokens, authorization codes,
         * client secrets, JWTs, or any sensitive values.
         */


        String encodedMessage =
                URLEncoder.encode(
                        message,
                        StandardCharsets.UTF_8
                );


        log.info(
                "event=OAUTH2_FAILURE_REDIRECT destination=/login"
        );


        response.sendRedirect(
                "http://localhost:3000/login"
                        + "?oauthError="
                        + encodedMessage
        );
    }
}