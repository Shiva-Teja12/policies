package com.example.hrmspolicies2.dto.response;

import com.example.hrmspolicies2.enums.NotificationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {

    private Long id;

    private Long policyId;
    private String policyCode;
    private String policyName;

    private String title;
    private String message;

    private NotificationStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
}