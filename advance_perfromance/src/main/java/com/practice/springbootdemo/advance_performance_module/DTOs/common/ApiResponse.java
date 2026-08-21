package com.practice.springbootdemo.advance_performance_module.DTOs.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard API Response Wrapper")
public class ApiResponse<T> {

    @Schema(description = "Indicates if request succeeded", example = "true")
    private boolean success;

    @Schema(description = "Human-readable message describing the outcome", example = "Operation completed successfully")
    private String message;

    @Schema(description = "Payload returned by the API")
    private T data;

    @Builder.Default
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Timestamp of the response", example = "2026-08-18T14:30:00")
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success("Request processed successfully", data);
    }

    public static <T> ApiResponse<T> success(String message) {
        return success(message, null);
    }
}