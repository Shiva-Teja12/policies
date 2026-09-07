package com.example.hrmspolicies2.service;

import com.example.hrmspolicies2.dto.ApprovalActionRequest;
import com.example.hrmspolicies2.dto.response.ApprovalResponse;
import com.example.hrmspolicies2.dto.response.PageResponse;
import com.example.hrmspolicies2.entity.Notification;
import com.example.hrmspolicies2.entity.Policy;
import com.example.hrmspolicies2.entity.PolicyApproval;
import com.example.hrmspolicies2.entity.User;
import com.example.hrmspolicies2.enums.*;
import com.example.hrmspolicies2.exception.BadRequestException;
import com.example.hrmspolicies2.exception.ForbiddenException;
import com.example.hrmspolicies2.exception.ResourceNotFoundException;
import com.example.hrmspolicies2.exception.UnauthorizedException;
import com.example.hrmspolicies2.repository.NotificationRepository;
import com.example.hrmspolicies2.repository.PolicyApprovalRepository;
import com.example.hrmspolicies2.repository.PolicyRepository;
import com.example.hrmspolicies2.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PolicyApprovalService {

    // =========================================================
    // STRUCTURED LOGGING
    // =========================================================

    private static final Logger log =
            LoggerFactory.getLogger(
                    PolicyApprovalService.class
            );

    private final PolicyRepository policyRepository;
    private final PolicyApprovalRepository approvalRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;


    public PolicyApprovalService(
            PolicyRepository policyRepository,
            PolicyApprovalRepository approvalRepository,
            UserRepository userRepository,
            NotificationRepository notificationRepository
    ) {
        this.policyRepository = policyRepository;
        this.approvalRepository = approvalRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }


    // =========================================================
    // SUBMIT POLICY FOR REVIEW
    // =========================================================

    @Transactional
    public ApprovalResponse submitForReview(
            Long policyId
    ) {

        log.info(
                "event=POLICY_SUBMIT_FOR_REVIEW_REQUEST policyId={}",
                policyId
        );

        Policy policy =
                findPolicyForUpdate(
                        policyId
                );

        if (policy.getStatus() != PolicyStatus.DRAFT
                && policy.getStatus() != PolicyStatus.REJECTED) {

            log.warn(
                    "event=POLICY_SUBMIT_FOR_REVIEW_REJECTED policyId={} reason=INVALID_STATUS status={}",
                    policyId,
                    policy.getStatus()
            );

            throw new BadRequestException(
                    "Only DRAFT or REJECTED policies can be submitted for review"
            );
        }

        if (approvalRepository.existsByPolicy_IdAndDecision(
                policyId,
                ApprovalDecision.PENDING
        )) {

            log.warn(
                    "event=POLICY_SUBMIT_FOR_REVIEW_REJECTED policyId={} reason=PENDING_APPROVAL_EXISTS",
                    policyId
            );

            throw new BadRequestException(
                    "This policy already has a pending approval"
            );
        }

        policy.setStatus(
                PolicyStatus.LEGAL_REVIEW
        );

        PolicyApproval approval =
                createApproval(
                        policy,
                        ApprovalStage.LEGAL_REVIEW,
                        1
                );

        policyRepository.save(
                policy
        );

        PolicyApproval saved =
                approvalRepository.save(
                        approval
                );

        notifyRole(
                Role.LEGAL_REVIEWER,
                policy,
                "Policy requires Legal Review",
                policy.getCode()
                        + " - "
                        + policy.getName()
                        + " is waiting for your legal review."
        );

        log.info(
                "event=POLICY_SUBMITTED_FOR_REVIEW policyId={} approvalId={} stage={} status={}",
                policyId,
                saved.getId(),
                saved.getStage(),
                policy.getStatus()
        );

        return map(
                saved
        );
    }


    // =========================================================
    // GET APPROVAL QUEUE
    // =========================================================

    @Transactional(readOnly = true)
    public PageResponse<ApprovalResponse> getMyQueue(
            int page,
            int size,
            String direction
    ) {

        User currentUser =
                currentUser();

        ApprovalStage stage =
                stageForRole(
                        currentUser.getRole()
                );

        log.debug(
                "event=APPROVAL_QUEUE_REQUEST userId={} role={} stage={} page={} size={} direction={}",
                currentUser.getId(),
                currentUser.getRole(),
                stage,
                page,
                size,
                direction
        );

        int safePage =
                Math.max(
                        page,
                        0
                );

        int safeSize =
                Math.min(
                        Math.max(
                                size,
                                1
                        ),
                        100
                );

        Sort.Direction sortDirection =
                "asc".equalsIgnoreCase(
                        direction
                )
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        Pageable pageable =
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(
                                sortDirection,
                                "submittedAt"
                        )
                );

        Page<PolicyApproval> approvals =
                approvalRepository
                        .findByStageAndDecision(
                                stage,
                                ApprovalDecision.PENDING,
                                pageable
                        );

        log.debug(
                "event=APPROVAL_QUEUE_COMPLETED userId={} stage={} returned={} totalElements={}",
                currentUser.getId(),
                stage,
                approvals.getNumberOfElements(),
                approvals.getTotalElements()
        );

        return new PageResponse<>(
                approvals.map(
                        this::map
                )
        );
    }


    // =========================================================
    // GET APPROVAL HISTORY
    // =========================================================

    @Transactional(readOnly = true)
    public List<ApprovalResponse> getApprovalHistory(
            Long policyId
    ) {

        log.debug(
                "event=APPROVAL_HISTORY_REQUEST policyId={}",
                policyId
        );

        if (!policyRepository.existsById(
                policyId
        )) {

            log.warn(
                    "event=APPROVAL_HISTORY_FAILED policyId={} reason=POLICY_NOT_FOUND",
                    policyId
            );

            throw ResourceNotFoundException
                    .forEntity(
                            "Policy",
                            policyId
                    );
        }

        List<ApprovalResponse> history =
                approvalRepository
                        .findByPolicy_IdOrderBySubmittedAtAsc(
                                policyId
                        )
                        .stream()
                        .map(
                                this::map
                        )
                        .toList();

        log.debug(
                "event=APPROVAL_HISTORY_COMPLETED policyId={} records={}",
                policyId,
                history.size()
        );

        return history;
    }


    // =========================================================
    // APPROVE POLICY
    // =========================================================

    @Transactional
    public ApprovalResponse approve(
            Long policyId,
            ApprovalActionRequest request
    ) {

        User approver =
                currentUser();

        log.info(
                "event=POLICY_APPROVAL_REQUEST policyId={} approverUserId={} approverRole={}",
                policyId,
                approver.getId(),
                approver.getRole()
        );

        Policy policy =
                findPolicyForUpdate(
                        policyId
                );

        ApprovalStage expectedStage =
                stageForRole(
                        approver.getRole()
                );

        PolicyApproval pending =
                findPendingApproval(
                        policyId,
                        expectedStage
                );

        validatePolicyStatusForStage(
                policy,
                expectedStage
        );

        pending.setDecision(
                ApprovalDecision.APPROVED
        );

        pending.setApprover(
                approver
        );

        pending.setComments(
                cleanComments(
                        request.getComments()
                )
        );

        pending.setDecidedAt(
                LocalDateTime.now()
        );

        PolicyStatus previousStatus =
                policy.getStatus();

        moveToNextStage(
                policy
        );

        PolicyApproval saved =
                approvalRepository.save(
                        pending
                );

        policyRepository.save(
                policy
        );

        sendNextStageNotification(
                policy
        );

        log.info(
                "event=POLICY_APPROVED policyId={} approvalId={} stage={} approverUserId={} approverRole={} previousStatus={} newStatus={}",
                policyId,
                saved.getId(),
                expectedStage,
                approver.getId(),
                approver.getRole(),
                previousStatus,
                policy.getStatus()
        );

        return map(
                saved
        );
    }


    // =========================================================
    // REJECT POLICY
    // =========================================================

    @Transactional
    public ApprovalResponse reject(
            Long policyId,
            ApprovalActionRequest request
    ) {

        if (!StringUtils.hasText(
                request.getComments()
        )) {

            log.warn(
                    "event=POLICY_REJECTION_REJECTED policyId={} reason=MISSING_REJECTION_REASON",
                    policyId
            );

            throw new BadRequestException(
                    "Rejection reason is required"
            );
        }

        User approver =
                currentUser();

        log.info(
                "event=POLICY_REJECTION_REQUEST policyId={} approverUserId={} approverRole={}",
                policyId,
                approver.getId(),
                approver.getRole()
        );

        Policy policy =
                findPolicyForUpdate(
                        policyId
                );

        ApprovalStage expectedStage =
                stageForRole(
                        approver.getRole()
                );

        PolicyApproval pending =
                findPendingApproval(
                        policyId,
                        expectedStage
                );

        validatePolicyStatusForStage(
                policy,
                expectedStage
        );

        PolicyStatus previousStatus =
                policy.getStatus();

        pending.setDecision(
                ApprovalDecision.REJECTED
        );

        pending.setApprover(
                approver
        );

        pending.setComments(
                request.getComments()
                        .trim()
        );

        pending.setDecidedAt(
                LocalDateTime.now()
        );

        policy.setStatus(
                PolicyStatus.REJECTED
        );

        PolicyApproval saved =
                approvalRepository.save(
                        pending
                );

        policyRepository.save(
                policy
        );

        if (policy.getCreatedBy() != null) {

            createNotification(
                    policy.getCreatedBy(),
                    policy,
                    "Policy rejected",
                    policy.getCode()
                            + " - "
                            + policy.getName()
                            + " was rejected by "
                            + approver.getRole().name()
                            + ". Reason: "
                            + request.getComments()
                            .trim()
            );
        }

        log.info(
                "event=POLICY_REJECTED policyId={} approvalId={} stage={} approverUserId={} approverRole={} previousStatus={} newStatus={}",
                policyId,
                saved.getId(),
                expectedStage,
                approver.getId(),
                approver.getRole(),
                previousStatus,
                policy.getStatus()
        );

        return map(
                saved
        );
    }


    // =========================================================
    // MOVE TO NEXT APPROVAL STAGE
    // =========================================================

    private void moveToNextStage(
            Policy policy
    ) {

        PolicyStatus currentStatus =
                policy.getStatus();

        switch (policy.getStatus()) {

            case LEGAL_REVIEW -> {

                policy.setStatus(
                        PolicyStatus.HR_HEAD_REVIEW
                );

                approvalRepository.save(
                        createApproval(
                                policy,
                                ApprovalStage.HR_HEAD_REVIEW,
                                2
                        )
                );

                log.info(
                        "event=POLICY_APPROVAL_STAGE_CHANGED policyId={} from={} to={}",
                        policy.getId(),
                        currentStatus,
                        policy.getStatus()
                );
            }

            case HR_HEAD_REVIEW -> {

                if (policy.getApplicability()
                        == Applicability.ALL) {

                    policy.setStatus(
                            PolicyStatus.MD_REVIEW
                    );

                    approvalRepository.save(
                            createApproval(
                                    policy,
                                    ApprovalStage.MD_REVIEW,
                                    3
                            )
                    );

                } else {

                    policy.setStatus(
                            PolicyStatus.APPROVED
                    );
                }

                log.info(
                        "event=POLICY_APPROVAL_STAGE_CHANGED policyId={} from={} to={} applicability={}",
                        policy.getId(),
                        currentStatus,
                        policy.getStatus(),
                        policy.getApplicability()
                );
            }

            case MD_REVIEW -> {

                policy.setStatus(
                        PolicyStatus.APPROVED
                );

                log.info(
                        "event=POLICY_APPROVAL_STAGE_CHANGED policyId={} from={} to={}",
                        policy.getId(),
                        currentStatus,
                        policy.getStatus()
                );
            }

            default -> {

                log.warn(
                        "event=POLICY_APPROVAL_STAGE_CHANGE_REJECTED policyId={} reason=INVALID_STATUS status={}",
                        policy.getId(),
                        policy.getStatus()
                );

                throw new BadRequestException(
                        "Policy is not in an approvable status"
                );
            }
        }
    }


    // =========================================================
    // SEND NEXT STAGE NOTIFICATION
    // =========================================================

    private void sendNextStageNotification(
            Policy policy
    ) {

        switch (policy.getStatus()) {

            case HR_HEAD_REVIEW ->

                    notifyRole(
                            Role.HR_HEAD,
                            policy,
                            "Policy requires HR Head review",
                            policy.getCode()
                                    + " - "
                                    + policy.getName()
                                    + " passed Legal Review."
                    );

            case MD_REVIEW ->

                    notifyRole(
                            Role.MANAGING_DIRECTOR,
                            policy,
                            "Policy requires final approval",
                            policy.getCode()
                                    + " - "
                                    + policy.getName()
                                    + " requires final MD approval."
                    );

            case APPROVED -> {

                if (policy.getCreatedBy() != null) {

                    createNotification(
                            policy.getCreatedBy(),
                            policy,
                            "Policy approved",
                            policy.getCode()
                                    + " - "
                                    + policy.getName()
                                    + " is approved and ready to publish."
                    );
                }
            }

            default -> {
                // No notification is required.
            }
        }
    }


    // =========================================================
    // CREATE APPROVAL
    // =========================================================

    private PolicyApproval createApproval(
            Policy policy,
            ApprovalStage stage,
            int sequence
    ) {

        log.debug(
                "event=APPROVAL_RECORD_CREATE policyId={} stage={} sequence={}",
                policy.getId(),
                stage,
                sequence
        );

        return PolicyApproval
                .builder()
                .policy(
                        policy
                )
                .stage(
                        stage
                )
                .decision(
                        ApprovalDecision.PENDING
                )
                .sequenceNumber(
                        sequence
                )
                .submittedAt(
                        LocalDateTime.now()
                )
                .build();
    }


    // =========================================================
    // FIND PENDING APPROVAL
    // =========================================================

    private PolicyApproval findPendingApproval(
            Long policyId,
            ApprovalStage stage
    ) {

        return approvalRepository
                .findFirstByPolicy_IdAndStageAndDecisionOrderBySubmittedAtDesc(
                        policyId,
                        stage,
                        ApprovalDecision.PENDING
                )
                .orElseThrow(
                        () -> {

                            log.warn(
                                    "event=PENDING_APPROVAL_NOT_FOUND policyId={} stage={}",
                                    policyId,
                                    stage
                            );

                            return new BadRequestException(
                                    "No pending "
                                            + stage.name()
                                            + " approval exists for this policy"
                            );
                        }
                );
    }


    // =========================================================
    // VALIDATE POLICY STATUS FOR STAGE
    // =========================================================

    private void validatePolicyStatusForStage(
            Policy policy,
            ApprovalStage stage
    ) {

        boolean valid =
                switch (stage) {

                    case LEGAL_REVIEW ->
                            policy.getStatus()
                                    == PolicyStatus.LEGAL_REVIEW;

                    case HR_HEAD_REVIEW ->
                            policy.getStatus()
                                    == PolicyStatus.HR_HEAD_REVIEW;

                    case MD_REVIEW ->
                            policy.getStatus()
                                    == PolicyStatus.MD_REVIEW;
                };

        if (!valid) {

            log.warn(
                    "event=APPROVAL_STAGE_VALIDATION_FAILED policyId={} expectedStage={} policyStatus={}",
                    policy.getId(),
                    stage,
                    policy.getStatus()
            );

            throw new BadRequestException(
                    "The policy is not at the "
                            + stage.name()
                            + " stage"
            );
        }
    }


    // =========================================================
    // MAP ROLE TO APPROVAL STAGE
    // =========================================================

    private ApprovalStage stageForRole(
            Role role
    ) {

        return switch (role) {

            case LEGAL_REVIEWER ->
                    ApprovalStage.LEGAL_REVIEW;

            case HR_HEAD ->
                    ApprovalStage.HR_HEAD_REVIEW;

            case MANAGING_DIRECTOR ->
                    ApprovalStage.MD_REVIEW;

            default -> {

                log.warn(
                        "event=APPROVAL_ACCESS_DENIED role={}",
                        role
                );

                throw new ForbiddenException(
                        "Your role does not have an approval queue"
                );
            }
        };
    }


    // =========================================================
    // FIND POLICY FOR UPDATE
    // =========================================================

    private Policy findPolicyForUpdate(
            Long policyId
    ) {

        return policyRepository
                .findByIdForUpdate(
                        policyId
                )
                .orElseThrow(
                        () -> {

                            log.warn(
                                    "event=POLICY_NOT_FOUND_FOR_APPROVAL policyId={}",
                                    policyId
                            );

                            return ResourceNotFoundException
                                    .forEntity(
                                            "Policy",
                                            policyId
                                    );
                        }
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
                || !authentication
                .isAuthenticated()) {

            log.warn(
                    "event=APPROVAL_AUTHENTICATION_FAILED reason=NO_AUTHENTICATED_USER"
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
                                    "event=APPROVAL_AUTHENTICATION_FAILED reason=USER_NOT_FOUND"
                            );

                            return new UnauthorizedException(
                                    "Authenticated user was not found"
                            );
                        }
                );
    }


    // =========================================================
    // NOTIFY ROLE
    // =========================================================

    private void notifyRole(
            Role role,
            Policy policy,
            String title,
            String message
    ) {

        List<User> recipients =
                userRepository
                        .findByRole(
                                role
                        );

        List<Notification> notifications =
                recipients
                        .stream()
                        .map(
                                user ->
                                        Notification
                                                .builder()
                                                .recipient(
                                                        user
                                                )
                                                .policy(
                                                        policy
                                                )
                                                .title(
                                                        title
                                                )
                                                .message(
                                                        message
                                                )
                                                .status(
                                                        NotificationStatus.PENDING
                                                )
                                                .build()
                        )
                        .toList();

        notificationRepository
                .saveAll(
                        notifications
                );

        log.debug(
                "event=APPROVAL_NOTIFICATION_CREATED policyId={} role={} recipientCount={}",
                policy.getId(),
                role,
                recipients.size()
        );
    }


    // =========================================================
    // CREATE NOTIFICATION
    // =========================================================

    private void createNotification(
            User recipient,
            Policy policy,
            String title,
            String message
    ) {

        notificationRepository.save(
                Notification
                        .builder()
                        .recipient(
                                recipient
                        )
                        .policy(
                                policy
                        )
                        .title(
                                title
                        )
                        .message(
                                message
                        )
                        .status(
                                NotificationStatus.PENDING
                        )
                        .build()
        );

        log.debug(
                "event=APPROVAL_NOTIFICATION_CREATED policyId={} recipientUserId={}",
                policy.getId(),
                recipient.getId()
        );
    }


    // =========================================================
    // CLEAN COMMENTS
    // =========================================================

    private String cleanComments(
            String comments
    ) {

        return StringUtils.hasText(
                comments
        )
                ? comments.trim()
                : null;
    }


    // =========================================================
    // ENTITY -> RESPONSE
    // =========================================================

    private ApprovalResponse map(
            PolicyApproval approval
    ) {

        User approver =
                approval.getApprover();

        Policy policy =
                approval.getPolicy();

        return ApprovalResponse
                .builder()
                .approvalId(
                        approval.getId()
                )
                .policyId(
                        policy.getId()
                )
                .policyCode(
                        policy.getCode()
                )
                .policyName(
                        policy.getName()
                )
                .policyStatus(
                        policy.getStatus()
                )
                .stage(
                        approval.getStage()
                )
                .decision(
                        approval.getDecision()
                )
                .sequenceNumber(
                        approval.getSequenceNumber()
                )
                .approverId(
                        approver == null
                                ? null
                                : approver.getId()
                )
                .approverName(
                        approver == null
                                ? null
                                : approver.getName()
                )
                .approverEmail(
                        approver == null
                                ? null
                                : approver.getEmail()
                )
                .comments(
                        approval.getComments()
                )
                .submittedAt(
                        approval.getSubmittedAt()
                )
                .decidedAt(
                        approval.getDecidedAt()
                )
                .build();
    }
}