package com.practice.springbootdemo.advance_performance_module.DTOs.goals;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Employee Goal Modification Submit Request")
public record GoalModificationSubmitRequest(
        @NotBlank(message = "Reason / comment is required")
        @Size(max = 2000, message = "Comment cannot exceed 2000 characters")
        String comment,
        @Size(max = 2000, message = "Requested changes cannot exceed 2000 characters")
        String requestedChanges
) {}
