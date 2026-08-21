package com.practice.springbootdemo.advance_performance_module.DTOs.hr;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Set Default Department Manager Request")
public record SetDefaultManagerRequest(
        @NotNull(message = "Manager ID is required")
        Long managerId
) {}
