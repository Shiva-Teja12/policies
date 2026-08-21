package com.practice.springbootdemo.advance_performance_module.DTOs.request;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ModificationRequestDto {
    private String requestedChanges;

    @NotBlank
    private String comment;
}
