package com.practice.springbootdemo.advance_performance_module.controller.manager;


import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/manager/goals")
@PreAuthorize("hasRole('MANAGER')")
@Tag(name = "Manager Goals", description = "Manager Goal Management & Tracking APIs")
@SecurityRequirement(name = "BearerAuth")
public class ManagerGoalController {
    private final ManagerGoalService goalService;
    private final SearchService searchService;
    public ManagerGoalController(ManagerGoalService goalService, SearchService searchService) {
        this.goalService = goalService;
        this.searchService = searchService;
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Assign Goal to Employee")
    public ApiResponse<GoalResponse> create(@Valid @RequestBody CreateGoalRequest request) {
        Long managerId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success("Goal assigned to employee successfully", goalService.create(request, managerId));
    }
    @PutMapping("/{id}")
    @Operation(summary = "Update Goal")
    public ApiResponse<GoalResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateGoalRequest request) {
        Long managerId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success("Goal updated successfully", goalService.update(id, request, managerId));
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete Goal")
    public void delete(@PathVariable Long id) {
        Long managerId = SecurityUtils.getCurrentUserId();
        goalService.delete(id, managerId);
    }
    @GetMapping("/{id}")
    public ApiResponse<GoalResponse> getGoal(@PathVariable Long id) {
        Long managerId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(goalService.getGoal(id, managerId));
    }
    @GetMapping
    public ApiResponse<List<GoalResponse>> getAllManagedGoals() {
        Long managerId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(goalService.getAllManagerGoals(managerId));
    }
    @GetMapping("/employee/{employeeId}")
    public ApiResponse<List<GoalResponse>> getEmployeeGoals(@PathVariable Long employeeId, @RequestParam Long cycleId) {
        Long managerId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(goalService.getEmployeeGoals(employeeId, cycleId, managerId));
    }
    @GetMapping("/search")
    @Operation(summary = "Search Managed Goals", description = "Dynamic multi-filter search, sort, and pagination for manager goals")
    public ApiResponse<PagedResponse<GoalResponse>> searchGoals(GoalSearchCriteria criteria) {
        Long managerId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(searchService.searchManagerGoals(criteria, managerId));
    }
}
