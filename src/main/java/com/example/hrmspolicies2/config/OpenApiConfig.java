package com.example.hrmspolicies2.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes interactive API docs at /swagger-ui.html and the raw OpenAPI
 * spec at /v3/api-docs. Both paths are permitted anonymously in
 * SecurityConfig. The "bearerAuth" scheme lets you paste a JWT once in
 * the Swagger UI "Authorize" dialog and have it sent on every request.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI hrmsPoliciesOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("HRMS Policies API")
                        .description("REST API for managing HR policies: authentication, CRUD, advanced search, filtering, sorting and pagination.")
                        .version("v1.0")
                        .contact(new Contact().name("HRMS Policies Team")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
