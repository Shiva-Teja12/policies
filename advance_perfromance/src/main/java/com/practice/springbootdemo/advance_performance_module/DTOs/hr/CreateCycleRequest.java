package com.practice.springbootdemo.advance_performance_module.DTOs.hr;


@Schema(description = "Create Performance Cycle Request")
public record CreateCycleRequest(
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