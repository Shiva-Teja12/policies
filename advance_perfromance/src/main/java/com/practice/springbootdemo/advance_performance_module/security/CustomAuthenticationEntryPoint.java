package com.practice.springbootdemo.advance_performance_module.security;

@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    public CustomAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        log.warn("401 Unauthorized access on URI: {} - Message: {}", request.getRequestURI(), authException.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse errorResponse = ErrorResponse.of(
                "Full authentication is required to access this resource. Please provide a valid JWT Bearer token.",
                "AUTHENTICATION_REQUIRED"
        );
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
