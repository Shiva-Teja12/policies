package com.example.hrmspolicies2.service;

import com.example.hrmspolicies2.dto.RetirePolicyRequest;
import com.example.hrmspolicies2.dto.response.PolicyRetirementResponse;
import com.example.hrmspolicies2.entity.Notification;
import com.example.hrmspolicies2.entity.Policy;
import com.example.hrmspolicies2.entity.PolicyAssignment;
import com.example.hrmspolicies2.enums.AssignmentStatus;
import com.example.hrmspolicies2.enums.NotificationStatus;
import com.example.hrmspolicies2.enums.PolicyStatus;
import com.example.hrmspolicies2.exception.BadRequestException;
import com.example.hrmspolicies2.exception.ResourceNotFoundException;
import com.example.hrmspolicies2.repository.NotificationRepository;
import com.example.hrmspolicies2.repository.PolicyAssignmentRepository;
import com.example.hrmspolicies2.repository.PolicyRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

@Service
public class PolicyRetirementService {

    // =========================================================
    // STRUCTURED LOGGING
    // =========================================================

    private static final Logger log =
            LoggerFactory.getLogger(
                    PolicyRetirementService.class
            );

    private final PolicyRepository policyRepository;
    private final PolicyAssignmentRepository assignmentRepository;
    private final NotificationRepository notificationRepository;


    public PolicyRetirementService(
            PolicyRepository policyRepository,
            PolicyAssignmentRepository assignmentRepository,
            NotificationRepository notificationRepository
    ) {
        this.policyRepository =
                policyRepository;

        this.assignmentRepository =
                assignmentRepository;

        this.notificationRepository =
                notificationRepository;
    }


    // =========================================================
    // RETIRE POLICY
    // =========================================================

    @Transactional
    public PolicyRetirementResponse retire(
            Long policyId,
            RetirePolicyRequest request
    ) {

        log.info(
                "event=POLICY_RETIRE_REQUEST policyId={} retirementEffectiveDate={}",
                policyId,
                request.getRetirementEffectiveDate()
        );

        Policy policy =
                policyRepository
                        .findByIdForUpdate(
                                policyId
                        )
                        .orElseThrow(
                                () -> {

                                    log.warn(
                                            "event=POLICY_RETIRE_FAILED policyId={} reason=POLICY_NOT_FOUND",
                                            policyId
                                    );

                                    return ResourceNotFoundException
                                            .forEntity(
                                                    "Policy",
                                                    policyId
                                            );
                                }
                        );

        if (policy.getStatus()
                == PolicyStatus.RETIRED) {

            log.warn(
                    "event=POLICY_RETIRE_REJECTED policyId={} reason=ALREADY_RETIRED",
                    policyId
            );

            throw new BadRequestException(
                    "This policy is already retired"
            );
        }

        if (policy.getStatus()
                != PolicyStatus.PUBLISHED) {

            log.warn(
                    "event=POLICY_RETIRE_REJECTED policyId={} reason=INVALID_STATUS status={}",
                    policyId,
                    policy.getStatus()
            );

            throw new BadRequestException(
                    "Only a PUBLISHED policy can be retired"
            );
        }

        policy.setStatus(
                PolicyStatus.RETIRED
        );

        policy.setRetirementReason(
                request.getReason()
                        .trim()
        );

        policy.setRetirementEffectiveDate(
                request.getRetirementEffectiveDate()
        );

        policyRepository.save(
                policy
        );

        log.info(
                "event=POLICY_STATUS_CHANGED policyId={} newStatus={}",
                policyId,
                policy.getStatus()
        );

        List<PolicyAssignment> openAssignments =
                assignmentRepository
                        .findByPolicy_IdAndStatusIn(
                                policyId,
                                EnumSet.of(
                                        AssignmentStatus.PENDING,
                                        AssignmentStatus.OVERDUE
                                )
                        );

        log.info(
                "event=POLICY_RETIRE_OPEN_ASSIGNMENTS_FOUND policyId={} openAssignments={}",
                policyId,
                openAssignments.size()
        );

        for (PolicyAssignment assignment :
                openAssignments) {

            assignment.setStatus(
                    AssignmentStatus.CANCELLED
            );

            assignmentRepository.save(
                    assignment
            );

            notificationRepository.save(
                    Notification
                            .builder()
                            .recipient(
                                    assignment.getEmployee()
                            )
                            .policy(
                                    policy
                            )
                            .title(
                                    "Policy retired"
                            )
                            .message(
                                    policy.getCode()
                                            + " - "
                                            + policy.getName()
                                            + " was retired. "
                                            + "Acknowledgement is no longer required."
                            )
                            .status(
                                    NotificationStatus.PENDING
                            )
                            .build()
            );

            log.debug(
                    "event=POLICY_ASSIGNMENT_CANCELLED policyId={} assignmentId={} employeeId={}",
                    policyId,
                    assignment.getId(),
                    assignment.getEmployee()
                            .getId()
            );
        }

        log.info(
                "event=POLICY_RETIRED policyId={} status={} cancelledAssignments={} retirementEffectiveDate={}",
                policy.getId(),
                policy.getStatus(),
                openAssignments.size(),
                policy.getRetirementEffectiveDate()
        );

        return PolicyRetirementResponse
                .builder()

                .policyId(
                        policy.getId()
                )

                .policyCode(
                        policy.getCode()
                )

                .policyName(
                        policy.getName()
                )

                .status(
                        policy.getStatus()
                )

                .retirementReason(
                        policy.getRetirementReason()
                )

                .retirementEffectiveDate(
                        policy.getRetirementEffectiveDate()
                )

                .cancelledAssignments(
                        (long) openAssignments.size()
                )

                .build();
    }
}