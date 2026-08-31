package com.example.hrmspolicies2.controller;

import com.example.hrmspolicies2.dto.response.*;
import com.example.hrmspolicies2.service.ComplianceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/policies/compliance-dashboard")
public class ComplianceController {

    private final ComplianceService complianceService;

    public ComplianceController(
            ComplianceService complianceService
    ) {
        this.complianceService =
                complianceService;
    }

    @GetMapping
    @PreAuthorize("""
            hasAnyRole(
                'HR_ADMIN',
                'HR_HEAD'
            )
            """)
    public ResponseEntity<
            ApiResponse<
                    PageResponse<PolicyComplianceResponse>
                    >
            > getComplianceDashboard(
            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            Long policyId,

            @RequestParam(required = false)
            Long categoryId,

            @RequestParam(required = false)
            String department,

            @RequestParam(required = false)
            Boolean mandatory,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate toDate,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(
                    defaultValue = "policyName"
            )
            String sortBy,

            @RequestParam(
                    defaultValue = "asc"
            )
            String direction
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Compliance dashboard fetched successfully",
                        complianceService
                                .getComplianceDashboard(
                                        search,
                                        policyId,
                                        categoryId,
                                        department,
                                        mandatory,
                                        fromDate,
                                        toDate,
                                        page,
                                        size,
                                        sortBy,
                                        direction
                                )
                )
        );
    }

    @GetMapping("/export")
    @PreAuthorize("""
            hasAnyRole(
                'HR_ADMIN',
                'HR_HEAD'
            )
            """)
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            Long policyId,

            @RequestParam(required = false)
            Long categoryId,

            @RequestParam(required = false)
            String department,

            @RequestParam(required = false)
            Boolean mandatory,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate toDate
    ) {
        byte[] csv =
                complianceService.exportCsv(
                        search,
                        policyId,
                        categoryId,
                        department,
                        mandatory,
                        fromDate,
                        toDate
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"policy-compliance.csv\""
                )
                .contentType(
                        MediaType.parseMediaType(
                                "text/csv"
                        )
                )
                .body(csv);
    }
}