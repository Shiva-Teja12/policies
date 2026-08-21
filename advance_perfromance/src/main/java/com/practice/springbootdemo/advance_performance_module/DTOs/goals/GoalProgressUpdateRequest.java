package com.practice.springbootdemo.advance_performance_module.DTOs.goals;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Goal Progress Update Request")
public record GoalProgressUpdateRequest(
        @NotNull(message = "Progress percentage is required")
        @Min(value = 0, message = "Progress cannot be less than 0%")
        @Max(value = 100, message = "Progress cannot exceed 100%")
        Integer progress,
        @Size(max = 2000, message = "Comment cannot exceed 2000 characters")
        String comment
) {}