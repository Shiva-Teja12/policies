package com.example.hrmspolicies2.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PolicyAnswerSource {

    private Long policyId;
    private String policyCode;
    private String policyName;

    private Integer versionNumber;
    private String sectionReference;
    private String relevantExcerpt;

    private Integer relevanceScore;
}