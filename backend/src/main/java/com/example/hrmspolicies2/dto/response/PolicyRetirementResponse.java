package com.example.hrmspolicies2.dto.response;

import com.example.hrmspolicies2.enums.PolicyStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class PolicyRetirementResponse {

    private Long policyId;
    private String policyCode;
    private String policyName;

    private PolicyStatus status;

    private String retirementReason;
    private LocalDate retirementEffectiveDate;

    private Long cancelledAssignments;
}