package com.example.hrmspolicies2.controller;

import com.example.hrmspolicies2.dto.RetirePolicyRequest;
import com.example.hrmspolicies2.dto.response.ApiResponse;
import com.example.hrmspolicies2.dto.response.PolicyRetirementResponse;
import com.example.hrmspolicies2.service.PolicyRetirementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/policies")
public class PolicyRetirementController {

    private final PolicyRetirementService retirementService;

    public PolicyRetirementController(
            PolicyRetirementService retirementService
    ) {
        this.retirementService =
                retirementService;
    }

    @PatchMapping("/{id}/retire")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<
            ApiResponse<PolicyRetirementResponse>
            > retire(
            @PathVariable Long id,

            @Valid
            @RequestBody
            RetirePolicyRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Policy retired successfully",
                        retirementService.retire(
                                id,
                                request
                        )
                )
        );
    }
}