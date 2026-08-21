package com.practice.springbootdemo.advance_performance_module.DTOs.goals;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Manager Modification Review Decision Request")
public record ModificationReviewRequest(
        @Size(max = 2000, message = "Manager comment cannot exceed 2000 characters")
        String comment
) {}