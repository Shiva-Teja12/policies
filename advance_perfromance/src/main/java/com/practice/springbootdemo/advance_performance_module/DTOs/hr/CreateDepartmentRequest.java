package com.practice.springbootdemo.advance_performance_module.DTOs.hr;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Create Department Request")
public record CreateDepartmentRequest(
        @NotBlank(message = "Department name is required")
        @Size(max = 100, message = "Department name cannot exceed 100 characters")
        String name,
        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description
) {}
