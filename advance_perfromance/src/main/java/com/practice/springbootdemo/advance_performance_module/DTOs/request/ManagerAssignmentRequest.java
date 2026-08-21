package com.practice.springbootdemo.advance_performance_module.DTOs.request;

import jakarta.validation.constraints.NotNull;

public class ManagerAssignmentRequest {


    @NotNull
    private Long managerId;

    @NotNull
    private Long employeeId;

    @NotNull
    private Long performanceCycleId;
}
