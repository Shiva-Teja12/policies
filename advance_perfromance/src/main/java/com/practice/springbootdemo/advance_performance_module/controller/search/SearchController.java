package com.practice.springbootdemo.advance_performance_module.controller.search;

import com.practice.springbootdemo.advance_performance_module.service.search.SearchService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Search & Filtering", description = "Advanced Dynamic Search, Pagination & Sorting APIs")
@SecurityRequirement(name = "BearerAuth")
public class SearchController {
    private final SearchService searchService;
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }
    @GetMapping("/employees/search")
    @PreAuthorize("hasAnyRole('HR', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Dynamic Employee Search", description = "General employee directory search with dynamic filters, sorting and 0-based pagination")
    public ApiResponse<PagedResponse<EmployeeResponse>> searchEmployees(EmployeeSearchCriteria criteria) {
        return ApiResponse.success(searchService.searchEmployees(criteria));
    }
    @GetMapping("/manager/goals/search")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Dynamic Managed Goals Search", description = "Dynamic search and pagination across manager's assigned team goals")
    public ApiResponse<PagedResponse<GoalResponse>> searchManagerGoals(GoalSearchCriteria criteria) {
        Long managerId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(searchService.searchManagerGoals(criteria, managerId));
    }