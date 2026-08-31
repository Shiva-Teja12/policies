package com.example.hrmspolicies2.exception;

/**
 * Thrown for authentication failures such as invalid credentials.
 * Mapped to HTTP 401 by {@link GlobalExceptionHandler}.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
