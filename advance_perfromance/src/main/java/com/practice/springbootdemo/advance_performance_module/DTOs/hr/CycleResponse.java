package com.practice.springbootdemo.advance_performance_module.DTOs.hr;

import com.practice.springbootdemo.advance_performance_module.entities.CycleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Performance Cycle Details Response")
public record CycleResponse(
        Long id,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        CycleStatus status,
        Long createdBy,
        LocalDateTime createdAt
) {}
