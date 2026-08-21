package com.practice.springbootdemo.advance_performance_module.DTOs.goals;

import com.practice.springbootdemo.advance_performance_module.entities.ModificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Goal Modification Request Detailed Record")
public record GoalModificationResponse(
        Long id,
        Long goalId,
        String goalTitle,
        Long employeeId,
        String employeeName,
        Long managerId,
        String managerName,
        String requestedChanges,
        String comment,
        ModificationStatus status,
        String managerComment,
        LocalDateTime requestedAt,
        LocalDateTime reviewedAt
) {}