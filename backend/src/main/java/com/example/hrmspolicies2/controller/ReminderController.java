package com.example.hrmspolicies2.controller;

import com.example.hrmspolicies2.dto.response.*;
import com.example.hrmspolicies2.service.PolicyReminderService;
import com.example.hrmspolicies2.service.ReminderHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/policies/reminders")
public class ReminderController {

    private final PolicyReminderService reminderService;
    private final ReminderHistoryService historyService;

    public ReminderController(
            PolicyReminderService reminderService,
            ReminderHistoryService historyService
    ) {
        this.reminderService =
                reminderService;

        this.historyService =
                historyService;
    }

    @PostMapping("/run")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<
            ApiResponse<Void>
            > runManually() {
        reminderService
                .processDailyReminders();

        return ResponseEntity.ok(
                ApiResponse.message(
                        "Policy reminder job completed"
                )
        );
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
                    PageResponse<ReminderLogResponse>
                    >
            > getHistory(
            @RequestParam(required = false)
            Long employeeId,

            @RequestParam(required = false)
            Long policyId,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "desc")
            String direction
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Reminder history fetched successfully",
                        historyService
                                .getReminderHistory(
                                        employeeId,
                                        policyId,
                                        page,
                                        size,
                                        direction
                                )
                )
        );
    }
}