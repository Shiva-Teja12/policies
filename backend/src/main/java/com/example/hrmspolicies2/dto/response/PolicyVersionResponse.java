package com.example.hrmspolicies2.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class PolicyVersionResponse {

    private Long id;

    private Long policyId;
    private String policyCode;
    private String policyName;

    private Integer versionNumber;
    private String content;

    private LocalDate effectiveDate;
    private LocalDateTime publishedAt;

    private Long publishedById;
    private String publishedByName;
    private String publishedByEmail;

    private String changeSummary;

    private Boolean currentVersion;
    private Boolean archived;

    private Integer assignedEmployees;
}