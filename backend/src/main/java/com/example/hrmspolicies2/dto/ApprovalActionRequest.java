package com.example.hrmspolicies2.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApprovalActionRequest {

    @Size(
            max = 2000,
            message = "Comments cannot exceed 2000 characters"
    )
    private String comments;
}