package com.practice.springbootdemo.advance_performance_module.DTOs.auth;


import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Public department item for registration dropdown")
public record PublicDepartmentResponse(
        Long id,
        String name
) {}
