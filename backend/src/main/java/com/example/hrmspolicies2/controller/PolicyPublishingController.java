package com.example.hrmspolicies2.controller;

import com.example.hrmspolicies2.dto.PublishPolicyRequest;
import com.example.hrmspolicies2.dto.response.ApiResponse;
import com.example.hrmspolicies2.dto.response.PolicyVersionResponse;
import com.example.hrmspolicies2.service.PolicyPublishingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
public class PolicyPublishingController {

    private final PolicyPublishingService publishingService;

    public PolicyPublishingController(
            PolicyPublishingService publishingService
    ) {
        this.publishingService =
                publishingService;
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<
            ApiResponse<PolicyVersionResponse>
            > publish(
            @PathVariable Long id,

            @Valid
            @RequestBody
            PublishPolicyRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Policy published successfully",
                        publishingService.publish(
                                id,
                                request
                        )
                )
        );
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<
            ApiResponse<
                    List<PolicyVersionResponse>
                    >
            > getVersions(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Policy versions fetched successfully",
                        publishingService
                                .getVersions(id)
                )
        );
    }

    @GetMapping(
            "/{id}/versions/{versionNumber}"
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<
            ApiResponse<PolicyVersionResponse>
            > getVersion(
            @PathVariable Long id,

            @PathVariable
            Integer versionNumber
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Policy version fetched successfully",
                        publishingService
                                .getVersion(
                                        id,
                                        versionNumber
                                )
                )
        );
    }
}