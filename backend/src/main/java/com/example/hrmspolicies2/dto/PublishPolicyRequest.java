package com.example.hrmspolicies2.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PublishPolicyRequest {

    @NotNull(
            message = "Effective date is required"
    )
    @FutureOrPresent(
            message = "Effective date cannot be in the past"
    )
    private LocalDate effectiveDate;

    @Size(
            max = 1000,
            message = "Change summary cannot exceed 1000 characters"
    )
    private String changeSummary;

    @Min(
            value = 1,
            message = "Acknowledgement period must be at least one day"
    )
    @Max(
            value = 365,
            message = "Acknowledgement period cannot exceed 365 days"
    )
    private Integer acknowledgementPeriodDays;
}