package com.practice.springbootdemo.advance_performance_module.DTOs.manager;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Manager Goal Update Request")
public record UpdateGoalRequest(
        @NotBlank(message = "Goal title is required")
        @Size(max = 200, message = "Title cannot exceed 200 characters")
        String title,
        @Size(max = 2000, message = "Description cannot exceed 2000 characters")
        String description,
        @Size(max = 1000, message = "Target cannot exceed 1000 characters")
        String target,
        @NotNull(message = "Weight is required")
        @DecimalMin(value = "0.01", message = "Weight must be at least 0.01%")
        @DecimalMax(value = "100.00", message = "Weight cannot exceed 100.00%")
        BigDecimal weight,
        LocalDate dueDate
) {}