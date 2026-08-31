package com.example.hrmspolicies2.exception;

/**
 * Thrown when the client sends invalid data that does not fit
 * a specific validation annotation (e.g. bad sort field, bad enum value).
 * Mapped to HTTP 400 by {@link GlobalExceptionHandler}.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
