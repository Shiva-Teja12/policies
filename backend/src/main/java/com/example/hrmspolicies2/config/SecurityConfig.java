package com.example.hrmspolicies2.config;

import com.example.hrmspolicies2.security.JwtAuthenticationFilter;
import com.example.hrmspolicies2.security.OAuth2AuthenticationFailureHandler;
import com.example.hrmspolicies2.security.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter
            jwtAuthenticationFilter;

    private final OAuth2AuthenticationSuccessHandler
            oauth2AuthenticationSuccessHandler;

    private final OAuth2AuthenticationFailureHandler
            oauth2AuthenticationFailureHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler,
            OAuth2AuthenticationFailureHandler oauth2AuthenticationFailureHandler
    ) {
        this.jwtAuthenticationFilter =
                jwtAuthenticationFilter;

        this.oauth2AuthenticationSuccessHandler =
                oauth2AuthenticationSuccessHandler;

        this.oauth2AuthenticationFailureHandler =
                oauth2AuthenticationFailureHandler;
    }

    // =========================================================
    // PASSWORD ENCODER
    // =========================================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // =========================================================
    // SPRING SECURITY
    // =========================================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                // =================================================
                // CSRF
                // =================================================

                .csrf(csrf ->
                        csrf.disable()
                )

                // =================================================
                // CORS
                // =================================================

                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource()
                        )
                )

                // =================================================
                // SESSION
                //
                // IF_REQUIRED is used because OAuth2 needs a
                // temporary session during the Google login flow.
                //
                // Protected APIs still use your JWT.
                // =================================================

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                )

                // =================================================
                // EXCEPTION HANDLING
                // =================================================

                .exceptionHandling(exception ->
                        exception

                                // ---------------------------------
                                // 401 - Not authenticated
                                // ---------------------------------

                                .authenticationEntryPoint(
                                        (
                                                request,
                                                response,
                                                error
                                        ) -> {

                                            response.setStatus(
                                                    HttpServletResponse
                                                            .SC_UNAUTHORIZED
                                            );

                                            response.setContentType(
                                                    "application/json"
                                            );

                                            response
                                                    .getWriter()
                                                    .write(
                                                            """
                                                            {
                                                              "success": false,
                                                              "status": 401,
                                                              "message": "Authentication is required"
                                                            }
                                                            """
                                                    );
                                        }
                                )

                                // ---------------------------------
                                // 403 - Authenticated but forbidden
                                // ---------------------------------

                                .accessDeniedHandler(
                                        (
                                                request,
                                                response,
                                                error
                                        ) -> {

                                            response.setStatus(
                                                    HttpServletResponse
                                                            .SC_FORBIDDEN
                                            );

                                            response.setContentType(
                                                    "application/json"
                                            );

                                            response
                                                    .getWriter()
                                                    .write(
                                                            """
                                                            {
                                                              "success": false,
                                                              "status": 403,
                                                              "message": "You do not have permission to access this resource"
                                                            }
                                                            """
                                                    );
                                        }
                                )
                )

                // =================================================
                // AUTHORIZATION
                // =================================================

                .authorizeHttpRequests(auth ->
                        auth

                                // ---------------------------------
                                // Public authentication APIs
                                // ---------------------------------

                                .requestMatchers(
                                        "/api/auth/**"
                                )
                                .permitAll()

                                // ---------------------------------
                                // Google OAuth endpoints
                                // ---------------------------------

                                .requestMatchers(
                                        "/oauth2/**",
                                        "/login/oauth2/**"
                                )
                                .permitAll()

                                // ---------------------------------
                                // Swagger
                                // ---------------------------------

                                .requestMatchers(
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/v3/api-docs/**"
                                )
                                .permitAll()

                                // ---------------------------------
                                // CREATE POLICY
                                // HR Admin only
                                // ---------------------------------

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/policies"
                                )
                                .hasRole(
                                        "HR_ADMIN"
                                )

                                // ---------------------------------
                                // UPDATE POLICY
                                // HR Admin only
                                // ---------------------------------

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/policies/**"
                                )
                                .hasRole(
                                        "HR_ADMIN"
                                )

                                // ---------------------------------
                                // POLICY WORKFLOW
                                // ---------------------------------

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/policies/**"
                                )
                                .hasAnyRole(
                                        "HR_ADMIN",
                                        "LEGAL_REVIEWER",
                                        "HR_HEAD",
                                        "MANAGING_DIRECTOR"
                                )

                                // ---------------------------------
                                // DELETE POLICY
                                // HR Admin only
                                // ---------------------------------

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/policies/**"
                                )
                                .hasRole(
                                        "HR_ADMIN"
                                )

                                // ---------------------------------
                                // Everything else needs login
                                // ---------------------------------

                                .anyRequest()
                                .authenticated()
                )

                // =================================================
                // GOOGLE OAUTH2 LOGIN
                // =================================================

                .oauth2Login(oauth ->
                        oauth

                                // Google login succeeded
                                .successHandler(
                                        oauth2AuthenticationSuccessHandler
                                )

                                // Google login failed/cancelled
                                .failureHandler(
                                        oauth2AuthenticationFailureHandler
                                )
                )

                // =================================================
                // JWT FILTER
                // =================================================

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // =========================================================
    // CORS CONFIGURATION
    // =========================================================

    @Bean
    public CorsConfigurationSource
    corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        // Next.js frontend
        configuration.setAllowedOrigins(
                List.of(
                        "http://localhost:3000"
                )
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of("*")
        );

        configuration.setExposedHeaders(
                List.of(
                        "Content-Disposition"
                )
        );

        configuration.setAllowCredentials(
                true
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}