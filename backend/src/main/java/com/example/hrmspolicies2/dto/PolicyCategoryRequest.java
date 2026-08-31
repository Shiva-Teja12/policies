package com.example.hrmspolicies2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PolicyCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Category code is required")
    @Pattern(
            regexp = "^[A-Z]+$",
            message = "Category code must contain uppercase letters only"
    )
    @Size(max = 30)
    private String code;

    @Size(max = 500)
    private String description;
}