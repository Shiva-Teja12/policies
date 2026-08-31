package com.example.hrmspolicies2.dto.response;

import com.example.hrmspolicies2.enums.AssignmentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class MyPolicyStatusResponse {

    private Long assignmentId;

    private Long policyId;
    private String policyCode;
    private String policyName;

    private Long categoryId;
    private String categoryName;

    private Boolean mandatory;

    private Long policyVersionId;
    private Integer versionNumber;

    private LocalDate effectiveDate;
    private LocalDate deadline;

    private Boolean acknowledged;
    private LocalDateTime acknowledgedAt;

    private Long daysOverdue;

    private AssignmentStatus assignmentStatus;
}