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

    @Transactional
    public ApprovalResponse submitForReview(
            Long policyId
    ) {
        Policy policy = findPolicyForUpdate(policyId);

        if (policy.getStatus() != PolicyStatus.DRAFT
                && policy.getStatus() != PolicyStatus.REJECTED) {
            throw new BadRequestException(
                    "Only DRAFT or REJECTED policies can be submitted for review"
            );
        }

        if (approvalRepository.existsByPolicy_IdAndDecision(
                policyId,
                ApprovalDecision.PENDING
        )) {
            throw new BadRequestException(
                    "This policy already has a pending approval"
            );
        }

        policy.setStatus(PolicyStatus.LEGAL_REVIEW);

        PolicyApproval approval = createApproval(
                policy,
                ApprovalStage.LEGAL_REVIEW,
                1
        );

        policyRepository.save(policy);

        PolicyApproval saved =
                approvalRepository.save(approval);

        notifyRole(
                Role.LEGAL_REVIEWER,
                policy,
                "Policy requires Legal Review",
                policy.getCode()
                        + " - "
                        + policy.getName()
                        + " is waiting for your legal review."
        );

        return map(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ApprovalResponse> getMyQueue(
            int page,
            int size,
            String direction
    ) {
        User currentUser = currentUser();

        ApprovalStage stage =
                stageForRole(currentUser.getRole());

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(
                Math.max(size, 1),
                100
        );

        Sort.Direction sortDirection =
                "asc".equalsIgnoreCase(direction)
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(
                        sortDirection,
                        "submittedAt"
                )
        );

        Page<PolicyApproval> approvals =
                approvalRepository.findByStageAndDecision(
                        stage,
                        ApprovalDecision.PENDING,
                        pageable
                );

        return new PageResponse<>(
                approvals.map(this::map)
        );
    }

    @Transactional(readOnly = true)
    public List<ApprovalResponse> getApprovalHistory(
            Long policyId
    ) {
        if (!policyRepository.existsById(policyId)) {
            throw ResourceNotFoundException.forEntity(
                    "Policy",
                    policyId
            );
        }

        return approvalRepository
                .findByPolicy_IdOrderBySubmittedAtAsc(
                        policyId
                )
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    public ApprovalResponse approve(
            Long policyId,
            ApprovalActionRequest request
    ) {
        User approver = currentUser();
        Policy policy = findPolicyForUpdate(policyId);

        ApprovalStage expectedStage =
                stageForRole(approver.getRole());

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

        pending.setApprover(approver);

        pending.setComments(
                cleanComments(request.getComments())
        );

        pending.setDecidedAt(
                LocalDateTime.now()
        );

        moveToNextStage(policy);

        PolicyApproval saved =
                approvalRepository.save(pending);

        policyRepository.save(policy);

        sendNextStageNotification(policy);

        return map(saved);
    }

    @Transactional
    public ApprovalResponse reject(
            Long policyId,
            ApprovalActionRequest request
    ) {
        if (!StringUtils.hasText(
                request.getComments()
        )) {
            throw new BadRequestException(
                    "Rejection reason is required"
            );
        }

        User approver = currentUser();
        Policy policy = findPolicyForUpdate(policyId);

        ApprovalStage expectedStage =
                stageForRole(approver.getRole());

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
                ApprovalDecision.REJECTED
        );

        pending.setApprover(approver);

        pending.setComments(
                request.getComments().trim()
        );

        pending.setDecidedAt(
                LocalDateTime.now()
        );

        policy.setStatus(
                PolicyStatus.REJECTED
        );

        PolicyApproval saved =
                approvalRepository.save(pending);

        policyRepository.save(policy);

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
                            + request.getComments().trim()
            );
        }

        return map(saved);
    }

    private void moveToNextStage(
            Policy policy
    ) {
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
            }

            case MD_REVIEW ->
                    policy.setStatus(
                            PolicyStatus.APPROVED
                    );

            default ->
                    throw new BadRequestException(
                            "Policy is not in an approvable status"
                    );
        }
    }

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

    private PolicyApproval createApproval(
            Policy policy,
            ApprovalStage stage,
            int sequence
    ) {
        return PolicyApproval.builder()
                .policy(policy)
                .stage(stage)
                .decision(
                        ApprovalDecision.PENDING
                )
                .sequenceNumber(sequence)
                .submittedAt(
                        LocalDateTime.now()
                )
                .build();
    }

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
                .orElseThrow(() ->
                        new BadRequestException(
                                "No pending "
                                        + stage.name()
                                        + " approval exists for this policy"
                        )
                );
    }

    private void validatePolicyStatusForStage(
            Policy policy,
            ApprovalStage stage
    ) {
        boolean valid = switch (stage) {
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
            throw new BadRequestException(
                    "The policy is not at the "
                            + stage.name()
                            + " stage"
            );
        }
    }

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

            default ->
                    throw new ForbiddenException(
                            "Your role does not have an approval queue"
                    );
        };
    }

    private Policy findPolicyForUpdate(
            Long policyId
    ) {
        return policyRepository
                .findByIdForUpdate(policyId)
                .orElseThrow(() ->
                        ResourceNotFoundException
                                .forEntity(
                                        "Policy",
                                        policyId
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

    private void notifyRole(
            Role role,
            Policy policy,
            String title,
            String message
    ) {
        List<User> recipients =
                userRepository.findByRole(role);

        List<Notification> notifications =
                recipients.stream()
                        .map(user ->
                                Notification.builder()
                                        .recipient(user)
                                        .policy(policy)
                                        .title(title)
                                        .message(message)
                                        .status(
                                                NotificationStatus.PENDING
                                        )
                                        .build()
                        )
                        .toList();

        notificationRepository.saveAll(
                notifications
        );
    }

    private void createNotification(
            User recipient,
            Policy policy,
            String title,
            String message
    ) {
        notificationRepository.save(
                Notification.builder()
                        .recipient(recipient)
                        .policy(policy)
                        .title(title)
                        .message(message)
                        .status(
                                NotificationStatus.PENDING
                        )
                        .build()
        );
    }

    private String cleanComments(
            String comments
    ) {
        return StringUtils.hasText(comments)
                ? comments.trim()
                : null;
    }

    private ApprovalResponse map(
            PolicyApproval approval
    ) {
        User approver =
                approval.getApprover();

        Policy policy =
                approval.getPolicy();

        return ApprovalResponse.builder()
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