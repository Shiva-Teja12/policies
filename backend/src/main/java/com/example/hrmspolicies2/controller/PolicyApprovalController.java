package com.example.hrmspolicies2.controller;

import com.example.hrmspolicies2.dto.ApprovalActionRequest;
import com.example.hrmspolicies2.dto.response.*;
import com.example.hrmspolicies2.service.PolicyApprovalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
public class PolicyApprovalController {

    private final PolicyApprovalService approvalService;

    public PolicyApprovalController(
            PolicyApprovalService approvalService
    ) {
        this.approvalService =
                approvalService;
    }

    @PatchMapping("/{id}/submit-for-review")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<
            ApiResponse<ApprovalResponse>
            > submitForReview(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Policy submitted for Legal Review",
                        approvalService
                                .submitForReview(id)
                )
        );
    }

    @GetMapping("/approvals/my-queue")
    @PreAuthorize("""
            hasAnyRole(
                'LEGAL_REVIEWER',
                'HR_HEAD',
                'MANAGING_DIRECTOR'
            )
            """)
    public ResponseEntity<
            ApiResponse<
                    PageResponse<ApprovalResponse>
                    >
            > getMyQueue(
            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "desc")
            String direction
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Approval queue fetched successfully",
                        approvalService.getMyQueue(
                                page,
                                size,
                                direction
                        )
                )
        );
    }

    @GetMapping("/{id}/approval-history")
    @PreAuthorize("""
            hasAnyRole(
                'HR_ADMIN',
                'LEGAL_REVIEWER',
                'HR_HEAD',
                'MANAGING_DIRECTOR'
            )
            """)
    public ResponseEntity<
            ApiResponse<
                    List<ApprovalResponse>
                    >
            > getApprovalHistory(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Approval history fetched successfully",
                        approvalService
                                .getApprovalHistory(id)
                )
        );
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("""
            hasAnyRole(
                'LEGAL_REVIEWER',
                'HR_HEAD',
                'MANAGING_DIRECTOR'
            )
            """)
    public ResponseEntity<
            ApiResponse<ApprovalResponse>
            > approve(
            @PathVariable Long id,

            @Valid
            @RequestBody
            ApprovalActionRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Policy approval completed successfully",
                        approvalService.approve(
                                id,
                                request
                        )
                )
        );
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("""
            hasAnyRole(
                'LEGAL_REVIEWER',
                'HR_HEAD',
                'MANAGING_DIRECTOR'
            )
            """)
    public ResponseEntity<
            ApiResponse<ApprovalResponse>
            > reject(
            @PathVariable Long id,

            @Valid
            @RequestBody
            ApprovalActionRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Policy rejected successfully",
                        approvalService.reject(
                                id,
                                request
                        )
                )
        );
    }
}