package com.practice.springbootdemo.advance_performance_module.DTOs.hr;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Manager Assignment Response")
public record AssignmentResponse(
        Long id,
        Long managerId,
        String managerName,
        String managerEmail,
        Long employeeId,
        String employeeName,
        String employeeEmail,
        Long performanceCycleId,
        boolean active,
        LocalDateTime assignedDate
) {}