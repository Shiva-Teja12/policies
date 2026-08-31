package com.example.hrmspolicies2.dto.response;

import com.example.hrmspolicies2.enums.ApprovalDecision;
import com.example.hrmspolicies2.enums.ApprovalStage;
import com.example.hrmspolicies2.enums.PolicyStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ApprovalResponse {

    private Long approvalId;

    private Long policyId;
    private String policyCode;
    private String policyName;
    private PolicyStatus policyStatus;

    private ApprovalStage stage;
    private ApprovalDecision decision;
    private Integer sequenceNumber;

    private Long approverId;
    private String approverName;
    private String approverEmail;

    private String comments;
    private LocalDateTime submittedAt;
    private LocalDateTime decidedAt;
}