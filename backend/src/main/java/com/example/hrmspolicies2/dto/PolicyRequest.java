package com.example.hrmspolicies2.dto;

import com.example.hrmspolicies2.enums.Applicability;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PolicyRequest {

    @NotBlank(message = "Policy name is required")
    @Size(
            max = 200,
            message = "Policy name cannot exceed 200 characters"
    )
    private String name;

    @NotBlank(message = "Policy code is required")
    @Pattern(
            regexp = "^ENF-[A-Z]+-\\d{3}$",
            message = "Policy code must follow ENF-CATEGORY-001 format"
    )
    private String code;

    @NotNull(message = "Policy category is required")
    private Long categoryId;

    @NotBlank(message = "Policy content is required")
    private String content;

    @NotNull(message = "Applicability is required")
    private Applicability applicability;

    private List<String> applicableDepartments;

    private List<String> applicableGrades;

    @NotNull(message = "Mandatory value is required")
    private Boolean mandatory;

    @Min(
            value = 1,
            message = "Acknowledgement period must be at least one day"
    )
    @Max(
            value = 365,
            message = "Acknowledgement period cannot exceed 365 days"
    )
    private Integer acknowledgementPeriodDays = 7;

    @Min(
            value = 1,
            message = "Onboarding period must be at least one day"
    )
    @Max(
            value = 365,
            message = "Onboarding period cannot exceed 365 days"
    )
    private Integer onboardingPeriodDays = 7;
}