package com.practice.springbootdemo.advance_performance_module.DTOs.hr;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Update Performance Cycle Request")
public record UpdateCycleRequest(
        @NotBlank(message = "Cycle name is required")
        @Size(max = 150, message = "Cycle name cannot exceed 150 characters")
        String name,
        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        String description,
        @NotNull(message = "Start date is required")
        LocalDate startDate,
        @NotNull(message = "End date is required")
        LocalDate endDate
) {}