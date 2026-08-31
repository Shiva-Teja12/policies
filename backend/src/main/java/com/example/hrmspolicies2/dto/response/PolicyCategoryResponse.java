package com.example.hrmspolicies2.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PolicyCategoryResponse {

    private Long id;
    private String name;
    private String code;
    private String description;
    private Boolean active;
}