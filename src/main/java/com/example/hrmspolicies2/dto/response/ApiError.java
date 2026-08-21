package com.example.hrmspolicies2.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard envelope for every error response returned by
 * {@link com.example.hrmspolicies2.exception.GlobalExceptionHandler} so that
 * clients (Postman, the frontend, etc.) always know what shape to expect:
 *
 * {
 *   "success": false,
 *   "message": "Policy not found with id: 42",
 *   "status": 404,
 *   "path": "/api/policies/42",
 *   "errors": null,
 *   "timestamp": "2026-08-18T10:15:30"
 * }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private boolean success = false;
    private String message;
    private int status;
    private String path;
    private List<String> errors;
    private LocalDateTime timestamp;

    public ApiError() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiError(String message, int status, String path) {
        this();
        this.message = message;
        this.status = status;
        this.path = path;
    }

    public ApiError(String message, int status, String path, List<String> errors) {
        this(message, status, path);
        this.errors = errors;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
