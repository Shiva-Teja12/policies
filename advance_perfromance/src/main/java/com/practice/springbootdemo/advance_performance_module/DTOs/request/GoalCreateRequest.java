package com.practice.springbootdemo.advance_performance_module.DTOs.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;


@Data
public class GoalCreateRequest {

    @NotNull
    private Long employeeId;

    @NotNull
    private Long performanceCycleId;

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    private String goalType;

    @NotNull @FutureOrPresent
    private LocalDate dueDate;

    @NotNull @Min(1) @Max(100)
    private Integer weight;
}
