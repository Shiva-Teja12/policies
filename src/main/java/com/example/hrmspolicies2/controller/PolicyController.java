package com.example.hrmspolicies2.controller;

import com.example.hrmspolicies2.dto.PolicyRequest;
import com.example.hrmspolicies2.dto.response.ApiResponse;
import com.example.hrmspolicies2.dto.response.PageResponse;
import com.example.hrmspolicies2.entity.Policy;
import com.example.hrmspolicies2.service.PolicyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Policies", description = "CRUD and search operations for HR policies")
public class PolicyController {

    private static final Logger log = LoggerFactory.getLogger(PolicyController.class);

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    // ==========================================
    // GET ALL
    // ==========================================

    @GetMapping
    @Operation(summary = "Get all policies", description = "Returns every policy, unpaginated. Use /search for filtering, sorting and pagination.")
    public ResponseEntity<ApiResponse<List<Policy>>> getAllPolicies() {
        List<Policy> policies = policyService.getAllPolicies();

        return ResponseEntity.ok(ApiResponse.success("Policies fetched successfully", policies));
    }

    // ==========================================
    // ADVANCED SEARCH: filters + sorting + pagination
    // ==========================================

    @GetMapping("/search")
    @Operation(
            summary = "Search policies",
            description = "Dynamic filtering by keyword/category/status/applicability/mandatory, plus sorting and pagination."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid sort field or parameter")
    })
    public ResponseEntity<ApiResponse<PageResponse<Policy>>> searchPolicies(
            @Parameter(description = "Free-text search across name, code and content")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "Exact category match, e.g. Leave, Conduct, Security")
            @RequestParam(required = false) String category,

            @Parameter(description = "Exact status match: DRAFT, ACTIVE, ARCHIVED")
            @RequestParam(required = false) String status,

            @Parameter(description = "Exact applicability match, e.g. ALL, MANAGERS, INTERNS")
            @RequestParam(required = false) String applicability,

            @Parameter(description = "Filter by whether the policy is mandatory")
            @RequestParam(required = false) Boolean mandatory,

            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of records per page")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Field to sort by: id, name, code, category, applicability, mandatory, status, createdAt, updatedAt")
            @RequestParam(defaultValue = "id") String sortBy,

            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "asc") String direction
    ) {
        PageResponse<Policy> result = policyService.searchPolicies(
                keyword, category, status, applicability, mandatory, page, size, sortBy, direction
        );

        return ResponseEntity.ok(ApiResponse.success("Policy search completed", result));
    }

    // ==========================================
    // GET BY ID
    // ==========================================

    @GetMapping("/{id}")
    @Operation(summary = "Get a policy by id")
    public ResponseEntity<ApiResponse<Policy>> getPolicy(@PathVariable Long id) {
        Policy policy = policyService.getPolicyById(id);

        return ResponseEntity.ok(ApiResponse.success("Policy fetched successfully", policy));
    }

    // ==========================================
    // CREATE
    // ==========================================

    @PostMapping
    @Operation(summary = "Create a new policy")
    public ResponseEntity<ApiResponse<Policy>> createPolicy(@Valid @RequestBody PolicyRequest request) {
        Policy created = policyService.createPolicy(request);

        log.info("POST /api/policies -> created policy code={}", created.getCode());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Policy created successfully", created));
    }

    // ==========================================
    // PUT (full update)
    // ==========================================

    @PutMapping("/{id}")
    @Operation(summary = "Fully update an existing policy")
    public ResponseEntity<ApiResponse<Policy>> updatePolicy(
            @PathVariable Long id,
            @Valid @RequestBody PolicyRequest request
    ) {
        Policy updated = policyService.updatePolicy(id, request);

        return ResponseEntity.ok(ApiResponse.success("Policy updated successfully", updated));
    }

    // ==========================================
    // PATCH (partial update)
    // ==========================================

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update an existing policy")
    public ResponseEntity<ApiResponse<Policy>> patchPolicy(
            @PathVariable Long id,
            @RequestBody PolicyRequest request
    ) {
        Policy patched = policyService.patchPolicy(id, request);

        return ResponseEntity.ok(ApiResponse.success("Policy patched successfully", patched));
    }

    // ==========================================
    // DELETE
    // ==========================================

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a policy")
    public ResponseEntity<ApiResponse<Void>> deletePolicy(@PathVariable Long id) {
        policyService.deletePolicy(id);

        log.info("DELETE /api/policies/{} -> deleted", id);

        return ResponseEntity.ok(ApiResponse.message("Policy deleted successfully"));
    }
}
