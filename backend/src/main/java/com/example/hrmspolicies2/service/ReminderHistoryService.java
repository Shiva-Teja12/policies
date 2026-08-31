package com.example.hrmspolicies2.service;

import com.example.hrmspolicies2.dto.response.PageResponse;
import com.example.hrmspolicies2.dto.response.ReminderLogResponse;
import com.example.hrmspolicies2.entity.ReminderLog;
import com.example.hrmspolicies2.repository.ReminderLogRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReminderHistoryService {

    private final ReminderLogRepository repository;

    public ReminderHistoryService(
            ReminderLogRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReminderLogResponse>
    getReminderHistory(
            Long employeeId,
            Long policyId,
            int page,
            int size,
            String direction
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(
                Math.max(size, 1),
                100
        );

        Sort.Direction sortDirection =
                "asc".equalsIgnoreCase(direction)
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        Pageable pageable =
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(
                                sortDirection,
                                "sentAt"
                        )
                );

        Page<ReminderLog> logs;

        if (employeeId != null) {
            logs = repository
                    .findByEmployee_Id(
                            employeeId,
                            pageable
                    );
        } else if (policyId != null) {
            logs = repository
                    .findByPolicy_Id(
                            policyId,
                            pageable
                    );
        } else {
            logs = repository.findAll(
                    pageable
            );
        }

        return new PageResponse<>(
                logs.map(this::map)
        );
    }

    private ReminderLogResponse map(
            ReminderLog log
    ) {
        return ReminderLogResponse
                .builder()
                .id(log.getId())
                .employeeId(
                        log.getEmployee()
                                .getId()
                )
                .employeeName(
                        log.getEmployee()
                                .getName()
                )
                .employeeEmail(
                        log.getEmployee()
                                .getEmail()
                )
                .policyId(
                        log.getPolicy()
                                .getId()
                )
                .policyCode(
                        log.getPolicy()
                                .getCode()
                )
                .policyName(
                        log.getPolicy()
                                .getName()
                )
                .reminderStage(
                        log.getReminderStage()
                )
                .deliveryStatus(
                        log.getDeliveryStatus()
                )
                .recipientEmail(
                        log.getRecipientEmail()
                )
                .ccEmail(
                        log.getCcEmail()
                )
                .sentAt(
                        log.getSentAt()
                )
                .failureReason(
                        log.getFailureReason()
                )
                .build();
    }
}