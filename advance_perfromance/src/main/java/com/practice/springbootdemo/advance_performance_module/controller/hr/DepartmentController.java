package com.practice.springbootdemo.advance_performance_module.controller.hr;


import com.practice.springbootdemo.advance_performance_module.DTOs.common.ApiResponse;
import com.practice.springbootdemo.advance_performance_module.DTOs.hr.CreateDepartmentRequest;
import com.practice.springbootdemo.advance_performance_module.DTOs.hr.DepartmentResponse;
import com.practice.springbootdemo.advance_performance_module.DTOs.hr.EmployeeResponse;
import com.practice.springbootdemo.advance_performance_module.DTOs.hr.SetDefaultManagerRequest;
import com.practice.springbootdemo.advance_performance_module.service.hr.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr/departments")
@PreAuthorize("hasRole('HR')")
@Tag(name = "Departments", description = "HR Department Management APIs")
@SecurityRequirement(name = "BearerAuth")
public class DepartmentController {
    private final DepartmentService service;

    public DepartmentController(DepartmentService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create Department", description = "Create a new organization department (HR only)")
    public ApiResponse<DepartmentResponse> create(@Valid @RequestBody CreateDepartmentRequest request) {
        return ApiResponse.success("Department created successfully", service.create(request));
    }

    @GetMapping
    @Operation(summary = "List Departments", description = "Retrieve all departments with assigned default managers (HR only)")
    public ApiResponse<List<DepartmentResponse>> list() {
        return ApiResponse.success(service.list());
    }

    @PutMapping("/{departmentId}/default-manager")
    @Operation(summary = "Set Default Manager", description = "Configure default manager for a department (HR only)")
    public ApiResponse<DepartmentResponse> setDefaultManager(
            @PathVariable Long departmentId,
            @Valid @RequestBody SetDefaultManagerRequest request
    ) {
        return ApiResponse.success("Default manager set successfully", service.setDefaultManager(departmentId, request.managerId()));
    }

    @GetMapping("/{departmentId}/employees")
    @Operation(summary = "Get Department Employees", description = "List all employees belonging to a specific department (HR only)")
    public ApiResponse<List<EmployeeResponse>> departmentEmployees(@PathVariable Long departmentId) {
        return ApiResponse.success(service.getDepartmentEmployees(departmentId));
    }
}
