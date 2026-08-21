package com.practice.springbootdemo.advance_performance_module.DTOs.hr;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Update Manager Assignment Request")
public record UpdateAssignmentRequest(
        @NotNull(message = "Manager ID is required")
        Long managerId
) {}