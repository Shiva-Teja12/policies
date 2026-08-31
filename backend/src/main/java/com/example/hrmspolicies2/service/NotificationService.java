package com.example.hrmspolicies2.service;

import com.example.hrmspolicies2.dto.response.NotificationResponse;
import com.example.hrmspolicies2.dto.response.PageResponse;
import com.example.hrmspolicies2.dto.response.UnreadNotificationResponse;
import com.example.hrmspolicies2.entity.Notification;
import com.example.hrmspolicies2.entity.Policy;
import com.example.hrmspolicies2.entity.User;
import com.example.hrmspolicies2.enums.NotificationStatus;
import com.example.hrmspolicies2.exception.ResourceNotFoundException;
import com.example.hrmspolicies2.exception.UnauthorizedException;
import com.example.hrmspolicies2.repository.NotificationRepository;
import com.example.hrmspolicies2.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;

@Service
public class NotificationService {

    private static final Set<String>
            SORTABLE_FIELDS = Set.of(
            "createdAt",
            "sentAt",
            "readAt",
            "status",
            "title"
    );

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository
    ) {
        this.notificationRepository =
                notificationRepository;

        this.userRepository =
                userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse>
    getMyNotifications(
            NotificationStatus status,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        User user = currentUser();

        String safeSortBy =
                SORTABLE_FIELDS.contains(sortBy)
                        ? sortBy
                        : "createdAt";

        Sort.Direction sortDirection =
                "asc".equalsIgnoreCase(direction)
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(
                        Math.max(size, 1),
                        100
                ),
                Sort.by(
                        sortDirection,
                        safeSortBy
                )
        );

        Page<Notification> notifications;

        if (status == null) {
            notifications =
                    notificationRepository
                            .findByRecipient_Id(
                                    user.getId(),
                                    pageable
                            );
        } else {
            notifications =
                    notificationRepository
                            .findByRecipient_IdAndStatus(
                                    user.getId(),
                                    status,
                                    pageable
                            );
        }

        return new PageResponse<>(
                notifications.map(this::map)
        );
    }

    @Transactional(readOnly = true)
    public UnreadNotificationResponse
    getUnreadCount() {
        User user = currentUser();

        long count =
                notificationRepository
                        .countByRecipient_IdAndStatusNot(
                                user.getId(),
                                NotificationStatus.READ
                        );

        return new UnreadNotificationResponse(
                count
        );
    }

    @Transactional
    public NotificationResponse markAsRead(
            Long notificationId
    ) {
        User user = currentUser();

        Notification notification =
                notificationRepository
                        .findByIdAndRecipient_Id(
                                notificationId,
                                user.getId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Notification was not found"
                                )
                        );

        notification.setStatus(
                NotificationStatus.READ
        );

        notification.setReadAt(
                LocalDateTime.now(
                        ZoneOffset.UTC
                )
        );

        return map(
                notificationRepository.save(
                        notification
                )
        );
    }

    private User currentUser() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new UnauthorizedException(
                    "Authentication is required"
            );
        }

        return userRepository
                .findByEmailIgnoreCase(
                        authentication.getName()
                )
                .orElseThrow(() ->
                        new UnauthorizedException(
                                "Authenticated user was not found"
                        )
                );
    }

    private NotificationResponse map(
            Notification notification
    ) {
        Policy policy =
                notification.getPolicy();

        return NotificationResponse.builder()
                .id(notification.getId())
                .policyId(
                        policy == null
                                ? null
                                : policy.getId()
                )
                .policyCode(
                        policy == null
                                ? null
                                : policy.getCode()
                )
                .policyName(
                        policy == null
                                ? null
                                : policy.getName()
                )
                .title(
                        notification.getTitle()
                )
                .message(
                        notification.getMessage()
                )
                .status(
                        notification.getStatus()
                )
                .createdAt(
                        notification.getCreatedAt()
                )
                .sentAt(
                        notification.getSentAt()
                )
                .readAt(
                        notification.getReadAt()
                )
                .build();
    }
}