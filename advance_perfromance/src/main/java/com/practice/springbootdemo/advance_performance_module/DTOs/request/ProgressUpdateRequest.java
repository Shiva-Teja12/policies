package com.practice.springbootdemo.advance_performance_module.DTOs.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProgressUpdateRequest {

    @NotNull
    @Min(0) @Max(100)
    private Integer progressPercentage;

    private String employeeComment;
}
