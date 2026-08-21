package com.practice.springbootdemo.advance_performance_module.DTOs.auth;

import com.practice.springbootdemo.advance_performance_module.entities.Role;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(description = "Authentication token & user details response")
public record LoginResponse(
        @Schema(description = "JWT Bearer Token")
        String token,
        Long id,
        String employeeCode,
        String name,
        String email,
        Role role,
        Long departmentId,
        String departmentName
) {}