package com.practice.springbootdemo.advance_performance_module.controller.hr;


import com.practice.springbootdemo.advance_performance_module.DTOs.common.ApiResponse;
import com.practice.springbootdemo.advance_performance_module.security.SecurityUtils;
import com.practice.springbootdemo.advance_performance_module.service.hr.PerformanceCycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hr/performance-cycles")
@Tag(name = "Performance Cycles", description = "HR Performance Cycle Lifecycle APIs")
@SecurityRequirement(name = "BearerAuth")
public class PerformanceCycleController {
    private final PerformanceCycleService service;
    public PerformanceCycleController(PerformanceCycleService service) {
        this.service = service;
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Create Performance Cycle", description = "HR creates a cycle in DRAFT status")
    public ApiResponse<CycleResponse> create(@Valid @RequestBody CreateCycleRequest request) {
        Long hrUserId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success("Performance cycle created successfully", service.create(request, hrUserId));
    }
    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get All Performance Cycles")
    public ApiResponse<List<CycleResponse>> getAll() {
        return ApiResponse.success(service.getAll());
    }
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('HR', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get Active Cycle")
    public ApiResponse<CycleResponse> getActive() {
        return ApiResponse.success(service.getActive());
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'MANAGER', 'EMPLOYEE')")
    public ApiResponse<CycleResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public ApiResponse<CycleResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateCycleRequest request) {
        return ApiResponse.success("Performance cycle updated successfully", service.update(id, request));
    }
    @PatchMapping("/{id}/launch")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Launch Performance Cycle", description = "Transitions cycle from DRAFT to ACTIVE")
    public ApiResponse<CycleResponse> launch(@PathVariable Long id) {
        return ApiResponse.success("Performance cycle launched successfully", service.activate(id));
    }
    @PatchMapping("/{id}/close")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Close Performance Cycle", description = "Transitions cycle to CLOSED")
    public ApiResponse<CycleResponse> close(@PathVariable Long id) {
        return ApiResponse.success("Performance cycle closed successfully", service.close(id));
    }
}
