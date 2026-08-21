package com.practice.springbootdemo.advance_performance_module.controller.manager;


import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/manager/goal-modification-requests")
@PreAuthorize("hasRole('MANAGER')")
@Tag(name = "Manager Goal Modification Review", description = "Manager Modification Review & Decision APIs")
@SecurityRequirement(name = "BearerAuth")
public class ManagerModificationController {
    private final GoalModificationService modificationService;
    public ManagerModificationController(GoalModificationService modificationService) {
        this.modificationService = modificationService;
    }
    @GetMapping
    @Operation(summary = "Get Pending Modification Requests")
    public ApiResponse<List<GoalModificationResponse>> getRequests(@RequestParam(required = false) ModificationStatus status) {
        Long managerId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(modificationService.getManagerModificationRequests(managerId, status));
    }
    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve Goal Modification Request")
    public ApiResponse<GoalModificationResponse> approve(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ModificationReviewRequest reviewRequest
    ) {
        Long managerId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success("Goal modification request approved successfully", modificationService.approveRequest(id, managerId, reviewRequest));
    }
    @PatchMapping("/{id}/reject")
    @Operation(summary = "Reject Goal Modification Request")
    public ApiResponse<GoalModificationResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ModificationReviewRequest reviewRequest
    ) {
        Long managerId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success("Goal modification request rejected", modificationService.rejectRequest(id, managerId, reviewRequest));
    }
