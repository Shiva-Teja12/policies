package com.example.hrmspolicies2.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RetirePolicyRequest {

    @NotBlank(
            message = "Retirement reason is required"
    )
    @Size(
            max = 1000,
            message = "Retirement reason cannot exceed 1000 characters"
    )
    private String reason;

    @NotNull(
            message = "Retirement effective date is required"
    )
    @FutureOrPresent(
            message = "Retirement effective date cannot be in the past"
    )
    private LocalDate retirementEffectiveDate;
}