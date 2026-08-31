package com.example.hrmspolicies2.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class PolicyComplianceResponse {

    private Long policyId;
    private String policyCode;
    private String policyName;

    private Long categoryId;
    private String categoryName;

    private Boolean mandatory;

    private Long policyVersionId;
    private Integer currentVersion;
    private LocalDate effectiveDate;

    private Long totalApplicableEmployees;
    private Long acknowledgedEmployees;
    private Long pendingEmployees;
    private Long overdueEmployees;

    private Double completionPercentage;

    private List<DepartmentComplianceResponse>
            departmentBreakdown;

    private List<OverdueEmployeeResponse>
            overdueEmployeeList;
}