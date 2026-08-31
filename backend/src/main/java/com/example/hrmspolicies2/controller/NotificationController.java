package com.example.hrmspolicies2.controller;

import com.example.hrmspolicies2.dto.response.*;
import com.example.hrmspolicies2.enums.NotificationStatus;
import com.example.hrmspolicies2.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(
            NotificationService notificationService
    ) {
        this.notificationService =
                notificationService;
    }

    @GetMapping
    public ResponseEntity<
            ApiResponse<
                    PageResponse<NotificationResponse>
                    >
            > getMyNotifications(
            @RequestParam(required = false)
            NotificationStatus status,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "createdAt")
            String sortBy,

            @RequestParam(defaultValue = "desc")
            String direction
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Notifications fetched successfully",
                        notificationService
                                .getMyNotifications(
                                        status,
                                        page,
                                        size,
                                        sortBy,
                                        direction
                                )
                )
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<
            ApiResponse<UnreadNotificationResponse>
            > getUnreadCount() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Unread notification count fetched successfully",
                        notificationService
                                .getUnreadCount()
                )
        );
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<
            ApiResponse<NotificationResponse>
            > markAsRead(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Notification marked as read",
                        notificationService
                                .markAsRead(id)
                )
        );
    }
}