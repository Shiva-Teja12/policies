package com.example.hrmspolicies2.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartmentComplianceResponse {

    private String department;

    private Long totalApplicableEmployees;
    private Long acknowledgedEmployees;
    private Long pendingEmployees;
    private Long overdueEmployees;

    private Double completionPercentage;
}