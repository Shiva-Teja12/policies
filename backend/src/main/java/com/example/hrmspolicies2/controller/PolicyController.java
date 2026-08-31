package com.example.hrmspolicies2.controller;

import com.example.hrmspolicies2.dto.PolicyRequest;
import com.example.hrmspolicies2.dto.response.*;
import com.example.hrmspolicies2.enums.Applicability;
import com.example.hrmspolicies2.enums.PolicyStatus;
import com.example.hrmspolicies2.service.PolicyService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(
            PolicyService policyService
    ) {
        this.policyService =
                policyService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<
            ApiResponse<
                    PageResponse<PolicyResponse>
                    >
            > searchPolicies(
            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            Long categoryId,

            @RequestParam(required = false)
            PolicyStatus status,

            @RequestParam(required = false)
            Applicability applicability,

            @RequestParam(required = false)
            Boolean mandatory,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "updatedAt")
            String sortBy,

            @RequestParam(defaultValue = "desc")
            String direction
    ) {
        PageResponse<PolicyResponse> result =
                policyService.searchPolicies(
                        search,
                        categoryId,
                        status,
                        applicability,
                        mandatory,
                        page,
                        size,
                        sortBy,
                        direction
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Policies fetched successfully",
                        result
                )
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<
            ApiResponse<PolicyResponse>
            > getPolicy(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Policy fetched successfully",
                        policyService
                                .getPolicyById(id)
                )
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<
            ApiResponse<PolicyResponse>
            > createPolicy(
            @Valid
            @RequestBody
            PolicyRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Policy created successfully",
                                policyService
                                        .createPolicy(
                                                request
                                        )
                        )
                );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<
            ApiResponse<PolicyResponse>
            > updatePolicy(
            @PathVariable Long id,

            @Valid
            @RequestBody
            PolicyRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Policy updated successfully",
                        policyService
                                .updatePolicy(
                                        id,
                                        request
                                )
                )
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<
            ApiResponse<Void>
            > deletePolicy(
            @PathVariable Long id
    ) {
        policyService.deletePolicy(id);

        return ResponseEntity.ok(
                ApiResponse.message(
                        "Policy deleted successfully"
                )
        );
    }
}