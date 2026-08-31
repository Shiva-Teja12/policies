package com.example.hrmspolicies2.dto.response;

import com.example.hrmspolicies2.enums.Applicability;
import com.example.hrmspolicies2.enums.PolicyStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PolicyResponse {

    private Long id;
    private String name;
    private String code;

    private Long categoryId;
    private String categoryName;
    private String categoryCode;

    private String content;
    private Applicability applicability;

    private List<String> applicableDepartments;
    private List<String> applicableGrades;

    private Boolean mandatory;
    private PolicyStatus status;

    private Integer acknowledgementPeriodDays;
    private Integer onboardingPeriodDays;

    private Boolean publishedOnce;
    private LocalDate effectiveDate;

    private Long createdById;
    private String createdByName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}