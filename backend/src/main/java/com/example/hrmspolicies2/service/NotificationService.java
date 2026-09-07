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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    // =========================================================
    // STRUCTURED LOGGING
    // =========================================================

    private static final Logger log =
            LoggerFactory.getLogger(
                    NotificationService.class
            );

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


    // =========================================================
    // GET MY NOTIFICATIONS
    // =========================================================

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse>
    getMyNotifications(
            NotificationStatus status,
            int page,
            int size,
            String sortBy,
            String direction
    ) {

        User user =
                currentUser();

        log.debug(
                "event=NOTIFICATION_LIST_REQUEST userId={} status={} page={} size={} sortBy={} direction={}",
                user.getId(),
                status,
                page,
                size,
                sortBy,
                direction
        );

        String safeSortBy =
                SORTABLE_FIELDS.contains(
                        sortBy
                )
                        ? sortBy
                        : "createdAt";


        Sort.Direction sortDirection =
                "asc".equalsIgnoreCase(
                        direction
                )
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;


        Pageable pageable =
                PageRequest.of(
                        Math.max(
                                page,
                                0
                        ),
                        Math.min(
                                Math.max(
                                        size,
                                        1
                                ),
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


        log.debug(
                "event=NOTIFICATION_LIST_COMPLETED userId={} returnedElements={} totalElements={} totalPages={}",
                user.getId(),
                notifications.getNumberOfElements(),
                notifications.getTotalElements(),
                notifications.getTotalPages()
        );


        return new PageResponse<>(
                notifications.map(
                        this::map
                )
        );
    }


    // =========================================================
    // GET UNREAD COUNT
    // =========================================================

    @Transactional(readOnly = true)
    public UnreadNotificationResponse
    getUnreadCount() {

        User user =
                currentUser();

        log.debug(
                "event=NOTIFICATION_UNREAD_COUNT_REQUEST userId={}",
                user.getId()
        );


        long count =
                notificationRepository
                        .countByRecipient_IdAndStatusNot(
                                user.getId(),
                                NotificationStatus.READ
                        );


        log.debug(
                "event=NOTIFICATION_UNREAD_COUNT_COMPLETED userId={} unreadCount={}",
                user.getId(),
                count
        );


        return new UnreadNotificationResponse(
                count
        );
    }


    // =========================================================
    // MARK NOTIFICATION AS READ
    // =========================================================

    @Transactional
    public NotificationResponse markAsRead(
            Long notificationId
    ) {

        User user =
                currentUser();


        log.info(
                "event=NOTIFICATION_MARK_READ_REQUEST notificationId={} userId={}",
                notificationId,
                user.getId()
        );


        Notification notification =
                notificationRepository
                        .findByIdAndRecipient_Id(
                                notificationId,
                                user.getId()
                        )
                        .orElseThrow(
                                () -> {

                                    log.warn(
                                            "event=NOTIFICATION_MARK_READ_FAILED notificationId={} userId={} reason=NOT_FOUND",
                                            notificationId,
                                            user.getId()
                                    );

                                    return new ResourceNotFoundException(
                                            "Notification was not found"
                                    );
                                }
                        );


        notification.setStatus(
                NotificationStatus.READ
        );


        notification.setReadAt(
                LocalDateTime.now(
                        ZoneOffset.UTC
                )
        );


        Notification savedNotification =
                notificationRepository.save(
                        notification
                );


        log.info(
                "event=NOTIFICATION_MARKED_READ notificationId={} userId={} status={}",
                savedNotification.getId(),
                user.getId(),
                savedNotification.getStatus()
        );


        return map(
                savedNotification
        );
    }


    // =========================================================
    // CURRENT USER
    // =========================================================

    private User currentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        if (authentication == null
                || !authentication.isAuthenticated()) {

            log.warn(
                    "event=NOTIFICATION_AUTHENTICATION_FAILED reason=NO_AUTHENTICATED_USER"
            );

            throw new UnauthorizedException(
                    "Authentication is required"
            );
        }


        return userRepository
                .findByEmailIgnoreCase(
                        authentication.getName()
                )
                .orElseThrow(
                        () -> {

                            log.warn(
                                    "event=NOTIFICATION_AUTHENTICATION_FAILED reason=USER_NOT_FOUND"
                            );

                            return new UnauthorizedException(
                                    "Authenticated user was not found"
                            );
                        }
                );
    }


    // =========================================================
    // MAP ENTITY TO RESPONSE
    // =========================================================

    private NotificationResponse map(
            Notification notification
    ) {

        Policy policy =
                notification.getPolicy();


        return NotificationResponse
                .builder()

                .id(
                        notification.getId()
                )

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