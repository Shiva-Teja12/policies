package com.example.hrmspolicies2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

/**
 * Request body for creating/updating a Policy.
 * Bean-validation annotations enforce basic input quality before the
 * request ever reaches the service layer.
 */
@Getter
@Setter
@Schema(description = "Payload used to create or update an HR policy")
public class PolicyRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must be at most 150 characters")
    @Schema(example = "Work From Home Policy")
    private String name;

    @NotBlank(message = "Code is required")
    @Size(max = 30, message = "Code must be at most 30 characters")
    @Schema(example = "WFH-001")
    private String code;

    @NotBlank(message = "Category is required")
    @Schema(example = "Leave")
    private String category;

    @Schema(example = "Employees may work remotely up to 3 days per week...")
    private String content;

    @NotBlank(message = "Applicability is required")
    @Schema(example = "ALL", description = "Who the policy applies to, e.g. ALL, MANAGERS, INTERNS")
    private String applicability;

    @NotNull(message = "Mandatory flag is required")
    @Schema(example = "true")
    private Boolean mandatory;

    @Pattern(regexp = "DRAFT|ACTIVE|ARCHIVED", message = "Status must be one of DRAFT, ACTIVE, ARCHIVED")
    @Schema(example = "DRAFT", allowableValues = {"DRAFT", "ACTIVE", "ARCHIVED"})
    private String status;
}
