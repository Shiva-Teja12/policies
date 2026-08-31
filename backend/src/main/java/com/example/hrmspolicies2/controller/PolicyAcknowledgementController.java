package com.example.hrmspolicies2.controller;

import com.example.hrmspolicies2.dto.response.*;
import com.example.hrmspolicies2.service.PolicyAcknowledgementService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
public class PolicyAcknowledgementController {

    private final PolicyAcknowledgementService acknowledgementService;

    public PolicyAcknowledgementController(
            PolicyAcknowledgementService acknowledgementService
    ) {
        this.acknowledgementService =
                acknowledgementService;
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<
            ApiResponse<AcknowledgementResponse>
            > acknowledge(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Policy acknowledged successfully",
                        acknowledgementService
                                .acknowledge(
                                        id,
                                        request
                                )
                )
        );
    }

    @GetMapping("/my-status")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<
            ApiResponse<
                    List<MyPolicyStatusResponse>
                    >
            > getMyStatus() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Employee policy status fetched successfully",
                        acknowledgementService
                                .getMyStatus()
                )
        );
    }
}