package com.example.hrmspolicies2.exception;

import com.example.hrmspolicies2.dto.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts exceptions thrown by the application into consistent
 * ApiError JSON responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // =========================================================
    // 404 - RESOURCE NOT FOUND
    // =========================================================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn(
                "Resource not found at [{}]: {}",
                request.getRequestURI(),
                ex.getMessage()
        );

        return build(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request
        );
    }

    // =========================================================
    // 409 - DUPLICATE RESOURCE
    // =========================================================

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicateResource(
            DuplicateResourceException ex,
            HttpServletRequest request
    ) {
        log.warn(
                "Duplicate resource conflict at [{}]: {}",
                request.getRequestURI(),
                ex.getMessage()
        );

        return build(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request
        );
    }

    // =========================================================
    // 401 - UNAUTHORIZED
    // Used when authentication fails.
    // Example: incorrect email or password.
    // =========================================================

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(
            UnauthorizedException ex,
            HttpServletRequest request
    ) {
        log.warn(
                "Unauthorized request at [{}]: {}",
                request.getRequestURI(),
                ex.getMessage()
        );

        return build(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                request
        );
    }

    // =========================================================
    // 403 - FORBIDDEN
    // Used when the user is authenticated but does not have
    // permission to use the selected portal or resource.
    // =========================================================

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(
            ForbiddenException ex,
            HttpServletRequest request
    ) {
        log.warn(
                "Forbidden request at [{}]: {}",
                request.getRequestURI(),
                ex.getMessage()
        );

        return build(
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                request
        );
    }

    // =========================================================
    // 400 - CUSTOM BAD REQUEST
    // =========================================================

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request
    ) {
        log.warn(
                "Bad request at [{}]: {}",
                request.getRequestURI(),
                ex.getMessage()
        );

        return build(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request
        );
    }

    // =========================================================
    // 400 - BEAN VALIDATION ERRORS
    // Handles errors from @Valid, @NotBlank, @Email, etc.
    // =========================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError ->
                        fieldError.getField()
                                + ": "
                                + fieldError.getDefaultMessage()
                )
                .collect(Collectors.toList());

        log.warn(
                "Validation failed at [{}]: {}",
                request.getRequestURI(),
                errors
        );

        ApiError error = new ApiError(
                "Validation failed for one or more fields",
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                errors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    // =========================================================
    // 400 - MISSING REQUEST PARAMETER
    // =========================================================

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        String message =
                "Missing required parameter: "
                        + ex.getParameterName();

        log.warn(
                "Missing request parameter at [{}]: {}",
                request.getRequestURI(),
                message
        );

        return build(
                HttpStatus.BAD_REQUEST,
                message,
                request
        );
    }

    // =========================================================
    // 400 - INVALID REQUEST PARAMETER TYPE
    // Example: passing text where a Long ID is expected.
    // =========================================================

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String message =
                "Invalid value for parameter '"
                        + ex.getName()
                        + "': "
                        + ex.getValue();

        log.warn(
                "Request parameter type mismatch at [{}]: {}",
                request.getRequestURI(),
                message
        );

        return build(
                HttpStatus.BAD_REQUEST,
                message,
                request
        );
    }

    // =========================================================
    // 500 - UNHANDLED EXCEPTION
    // This method must remain near the end because it is the
    // fallback handler for unexpected exceptions.
    // =========================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error(
                "Unhandled exception at [{}]",
                request.getRequestURI(),
                ex
        );

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.",
                request
        );
    }

    // =========================================================
    // RESPONSE BUILDER
    // =========================================================

    private ResponseEntity<ApiError> build(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        ApiError error = new ApiError(
                message,
                status.value(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(status)
                .body(error);
    }
}