package com.practice.springbootdemo.advance_performance_module.DTOs.hr;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Assign Manager to Employee Request")
public record AssignManagerRequest(
        @NotNull(message = "Manager ID is required")
        Long managerId,
        @NotNull(message = "Employee ID is required")
        Long employeeId,
        Long performanceCycleId
) {}
