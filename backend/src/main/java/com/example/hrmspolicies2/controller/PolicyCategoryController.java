package com.example.hrmspolicies2.controller;

import com.example.hrmspolicies2.dto.PolicyCategoryRequest;
import com.example.hrmspolicies2.dto.response.*;
import com.example.hrmspolicies2.service.PolicyCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policy-categories")
public class PolicyCategoryController {

    private final PolicyCategoryService categoryService;

    public PolicyCategoryController(
            PolicyCategoryService categoryService
    ) {
        this.categoryService =
                categoryService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<
            ApiResponse<
                    List<PolicyCategoryResponse>
                    >
            > getCategories() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Policy categories fetched successfully",
                        categoryService
                                .getActiveCategories()
                )
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<
            ApiResponse<PolicyCategoryResponse>
            > createCategory(
            @Valid
            @RequestBody
            PolicyCategoryRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Policy category created successfully",
                                categoryService
                                        .createCategory(
                                                request
                                        )
                        )
                );
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<
            ApiResponse<Void>
            > deactivateCategory(
            @PathVariable Long id
    ) {
        categoryService
                .deactivateCategory(id);

        return ResponseEntity.ok(
                ApiResponse.message(
                        "Policy category deactivated successfully"
                )
        );
    }
}