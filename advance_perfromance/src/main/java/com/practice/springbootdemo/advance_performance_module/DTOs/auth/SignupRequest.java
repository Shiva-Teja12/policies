package com.practice.springbootdemo.advance_performance_module.DTOs.auth;

import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "Employee registration payload")
public record SignupRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,
        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,
        @NotBlank(message = "Confirm password is required")
        String confirmPassword,
        @NotNull(message = "Department ID is required")
        Long departmentId
) {}