package com.example.hrmspolicies2.exception;

/**
 * Thrown when an operation would violate a uniqueness constraint
 * (e.g. policy code already exists, email already registered).
 * Mapped to HTTP 409 by {@link GlobalExceptionHandler}.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
