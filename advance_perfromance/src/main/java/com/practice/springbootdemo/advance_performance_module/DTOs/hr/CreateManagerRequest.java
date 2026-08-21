package com.practice.springbootdemo.advance_performance_module.DTOs.hr;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Create Manager Account Request")
public record CreateManagerRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100)
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