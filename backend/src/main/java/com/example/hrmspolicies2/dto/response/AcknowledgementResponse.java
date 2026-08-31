package com.example.hrmspolicies2.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AcknowledgementResponse {

    private Long acknowledgementId;

    private Long employeeId;
    private String employeeName;
    private String employeeEmail;

    private Long policyId;
    private String policyCode;
    private String policyName;

    private Long policyVersionId;
    private Integer versionNumber;

    private LocalDateTime acknowledgedAt;
    private String ipAddress;
    private String userAgent;

    private Boolean updatedExistingAcknowledgement;
}