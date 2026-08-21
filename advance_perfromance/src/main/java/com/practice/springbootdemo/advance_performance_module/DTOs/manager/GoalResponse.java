package com.practice.springbootdemo.advance_performance_module.DTOs.manager;


import com.practice.springbootdemo.advance_performance_module.entities.GoalScope;
import com.practice.springbootdemo.advance_performance_module.entities.GoalStatus;
import com.practice.springbootdemo.advance_performance_module.entities.GoalType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Detailed Goal Response")
public record GoalResponse(
        Long id,
        Long cycleId,
        Long employeeId,
        String employeeName,
        Long managerId,
        String managerName,
        GoalType goalType,
        GoalScope goalScope,
        Long parentGoalId,
        String title,
        String description,
        String target,
        BigDecimal weight,
        LocalDate dueDate,
        GoalStatus status,
        Integer progress,
        String employeeComment,
        String managerComment,
        boolean modificationRequested,
        boolean employeeAccepted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt
) {}

