package com.example.hrmspolicies2.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class OverdueEmployeeResponse {

    private Long employeeId;
    private String employeeName;
    private String employeeEmail;
    private String department;

    private LocalDate deadline;
    private Long daysOverdue;
}