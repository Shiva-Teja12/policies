package com.example.hrmspolicies2.repository;

import com.example.hrmspolicies2.entity.Notification;
import com.example.hrmspolicies2.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    Page<Notification>
    findByRecipient_Id(
            Long recipientId,
            Pageable pageable
    );

    Page<Notification>
    findByRecipient_IdAndStatus(
            Long recipientId,
            NotificationStatus status,
            Pageable pageable
    );

    Optional<Notification>
    findByIdAndRecipient_Id(
            Long notificationId,
            Long recipientId
    );

    long countByRecipient_IdAndStatusNot(
            Long recipientId,
            NotificationStatus status
    );
}