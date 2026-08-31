package com.example.hrmspolicies2.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PolicyAnswerResponse {

    private String question;
    private String answer;

    private Boolean aiProviderConfigured;
    private String retrievalMode;

    private List<PolicyAnswerSource> sources;
}