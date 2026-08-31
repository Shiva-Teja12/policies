package com.example.hrmspolicies2.dto.response;

import com.example.hrmspolicies2.enums.NotificationStatus;
import com.example.hrmspolicies2.enums.ReminderStage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReminderLogResponse {

    private Long id;

    private Long employeeId;
    private String employeeName;
    private String employeeEmail;

    private Long policyId;
    private String policyCode;
    private String policyName;

    private ReminderStage reminderStage;
    private NotificationStatus deliveryStatus;

    private String recipientEmail;
    private String ccEmail;

    private LocalDateTime sentAt;
    private String failureReason;
}