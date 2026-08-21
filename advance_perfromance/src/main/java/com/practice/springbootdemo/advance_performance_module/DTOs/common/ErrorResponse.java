package com.practice.springbootdemo.advance_performance_module.DTOs.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard Error Response Structure")
public class ErrorResponse {

    @Builder.Default
    @Schema(description = "Success flag (always false for errors)", example = "false")
    private boolean success = false;

    @Schema(description = "High level error message", example = "Manager is not assigned to this employee")
    private String message;

    @Schema(description = "Application specific error code", example = "BUSINESS_AUTHORIZATION_DENIED")
    private String errorCode;

    @Schema(description = "Validation field errors or additional details")
    private Map<String, String> errors;

    @Builder.Default
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Timestamp when the error occurred", example = "2026-08-18T14:30:00")
    private LocalDateTime timestamp = LocalDateTime.now();

    public static ErrorResponse of(String message, String errorCode) {
        return ErrorResponse.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ErrorResponse of(String message, String errorCode, Map<String, String> errors) {
        return ErrorResponse.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }
}