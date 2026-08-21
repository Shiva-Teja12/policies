package com.practice.springbootdemo.advance_performance_module.DTOs.hr;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Department Response")
public record DepartmentResponse(
        Long id,
        String name,
        String description,
        Long defaultManagerId,
        String defaultManagerName
) {}