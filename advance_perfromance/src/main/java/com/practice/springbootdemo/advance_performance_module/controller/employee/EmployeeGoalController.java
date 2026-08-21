package com.practice.springbootdemo.advance_performance_module.controller.employee;

import com.practice.springbootdemo.advance_performance_module.DTOs.common.ApiResponse;
import com.practice.springbootdemo.advance_performance_module.DTOs.common.PagedResponse;
import com.practice.springbootdemo.advance_performance_module.DTOs.employee.EmployeeGoalResponse;
import com.practice.springbootdemo.advance_performance_module.DTOs.goals.GoalModificationResponse;
import com.practice.springbootdemo.advance_performance_module.DTOs.goals.GoalModificationSubmitRequest;
import com.practice.springbootdemo.advance_performance_module.DTOs.goals.GoalProgressUpdateRequest;
import com.practice.springbootdemo.advance_performance_module.DTOs.search.GoalSearchCriteria;
import com.practice.springbootdemo.advance_performance_module.security.SecurityUtils;
import com.practice.springbootdemo.advance_performance_module.service.employee.EmployeeGoalService;
import com.practice.springbootdemo.advance_performance_module.service.goals.GoalModificationService;
import com.practice.springbootdemo.advance_performance_module.service.search.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employee/goals")
@PreAuthorize("hasRole('EMPLOYEE')")
@Tag(name = "Employee Goals", description = "Employee Goal Acceptance, Progress & Modification APIs")
@SecurityRequirement(name = "BearerAuth")
public class EmployeeGoalController {
    private final EmployeeGoalService employeeGoalService;
    private final GoalModificationService modificationService;
    private final SearchService searchService;

    public EmployeeGoalController(
            EmployeeGoalService employeeGoalService,
            GoalModificationService modificationService,
            SearchService searchService
    ) {
        this.employeeGoalService = employeeGoalService;
        this.modificationService = modificationService;
        this.searchService = searchService;
    }

    @GetMapping
    @Operation(summary = "Get My Goals")
    public ApiResponse<List<EmployeeGoalResponse>> getMyGoals() {
        Long employeeId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(employeeGoalService.getMyGoals(employeeId));
    }

    @GetMapping("/{id}")
    public ApiResponse<EmployeeGoalResponse> getGoalById(@PathVariable Long id) {
        Long employeeId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(employeeGoalService.getGoalById(id, employeeId));
    }

    @PatchMapping("/{goalId}/accept")
    @Operation(summary = "Accept Assigned Goal")
    public ApiResponse<EmployeeGoalResponse> acceptGoal(@PathVariable Long goalId) {
        Long employeeId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success("Goal accepted successfully", employeeGoalService.acceptGoal(goalId, employeeId));
    }

    @PatchMapping("/{goalId}/modification-request")
    @Operation(summary = "Request Goal Modification")
    public ApiResponse<GoalModificationResponse> requestModification(
            @PathVariable Long goalId,
            @Valid @RequestBody GoalModificationSubmitRequest request
    ) {
        Long employeeId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success("Goal modification request submitted successfully", modificationService.submitRequest(goalId, employeeId, request));
    }

    @PatchMapping("/{goalId}/progress")
    @Operation(summary = "Update Goal Progress")
    public ApiResponse<EmployeeGoalResponse> updateProgress(
            @PathVariable Long goalId,
            @Valid @RequestBody GoalProgressUpdateRequest request
    ) {
        Long employeeId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success("Goal progress updated successfully", employeeGoalService.updateProgress(goalId, employeeId, request));
    }

    @GetMapping("/search")
    @Operation(summary = "Search My Goals", description = "Dynamic search, filter, sort, and pagination on employee goals")
    public ApiResponse<PagedResponse<EmployeeGoalResponse>> searchMyGoals(GoalSearchCriteria criteria) {
        Long employeeId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(searchService.searchEmployeeGoals(criteria, employeeId));
    }

    @GetMapping("/modification-requests")
    public ApiResponse<List<GoalModificationResponse>> getMyModificationRequests() {
        Long employeeId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(modificationService.getEmployeeModificationRequests(employeeId));
    }
}
