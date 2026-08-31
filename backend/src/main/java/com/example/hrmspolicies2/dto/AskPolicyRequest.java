package com.example.hrmspolicies2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AskPolicyRequest {

    @NotBlank(
            message = "Question is required"
    )
    @Size(
            min = 3,
            max = 1000,
            message = "Question must contain 3 to 1000 characters"
    )
    private String question;
}