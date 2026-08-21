package com.practice.springbootdemo.advance_performance_module.DTOs.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Employee registration result")
public record SignupResponse(
        Long id,
        String employeeCode,
        String name,
        String email,
        Long departmentId,
        String departmentName,
        String message
) {}