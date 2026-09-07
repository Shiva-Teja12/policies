package com.example.hrmspolicies2.service;

import com.example.hrmspolicies2.dto.response.AcknowledgementResponse;
import com.example.hrmspolicies2.dto.response.MyPolicyStatusResponse;
import com.example.hrmspolicies2.entity.*;
import com.example.hrmspolicies2.enums.AssignmentStatus;
import com.example.hrmspolicies2.enums.PolicyStatus;
import com.example.hrmspolicies2.enums.Role;
import com.example.hrmspolicies2.exception.BadRequestException;
import com.example.hrmspolicies2.exception.ForbiddenException;
import com.example.hrmspolicies2.exception.ResourceNotFoundException;
import com.example.hrmspolicies2.exception.UnauthorizedException;
import com.example.hrmspolicies2.repository.*;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class PolicyAcknowledgementService {

    // =========================================================
    // STRUCTURED LOGGING
    // =========================================================

    private static final Logger log =
            LoggerFactory.getLogger(
                    PolicyAcknowledgementService.class
            );

    private final PolicyRepository policyRepository;
    private final PolicyVersionRepository versionRepository;
    private final PolicyAssignmentRepository assignmentRepository;
    private final PolicyAcknowledgementRepository acknowledgementRepository;
    private final UserRepository userRepository;


    public PolicyAcknowledgementService(
            PolicyRepository policyRepository,
            PolicyVersionRepository versionRepository,
            PolicyAssignmentRepository assignmentRepository,
            PolicyAcknowledgementRepository acknowledgementRepository,
            UserRepository userRepository
    ) {
        this.policyRepository = policyRepository;
        this.versionRepository = versionRepository;
        this.assignmentRepository = assignmentRepository;
        this.acknowledgementRepository = acknowledgementRepository;
        this.userRepository = userRepository;
    }


    // =========================================================
    // ACKNOWLEDGE POLICY
    // =========================================================

    @Transactional
    public AcknowledgementResponse acknowledge(
            Long policyId,
            HttpServletRequest request
    ) {

        User employee =
                currentEmployee();

        log.info(
                "event=POLICY_ACKNOWLEDGEMENT_REQUEST policyId={} employeeId={}",
                policyId,
                employee.getId()
        );

        Policy policy =
                policyRepository
                        .findById(
                                policyId
                        )
                        .orElseThrow(
                                () -> {

                                    log.warn(
                                            "event=POLICY_ACKNOWLEDGEMENT_FAILED policyId={} employeeId={} reason=POLICY_NOT_FOUND",
                                            policyId,
                                            employee.getId()
                                    );

                                    return ResourceNotFoundException
                                            .forEntity(
                                                    "Policy",
                                                    policyId
                                            );
                                }
                        );

        if (policy.getStatus()
                != PolicyStatus.PUBLISHED) {

            log.warn(
                    "event=POLICY_ACKNOWLEDGEMENT_REJECTED policyId={} employeeId={} reason=INVALID_STATUS status={}",
                    policyId,
                    employee.getId(),
                    policy.getStatus()
            );

            throw new BadRequestException(
                    "Only a currently published policy can be acknowledged"
            );
        }

        PolicyVersion currentVersion =
                versionRepository
                        .findByPolicy_IdAndCurrentVersionTrue(
                                policyId
                        )
                        .orElseThrow(
                                () -> {

                                    log.warn(
                                            "event=POLICY_ACKNOWLEDGEMENT_FAILED policyId={} employeeId={} reason=CURRENT_VERSION_NOT_FOUND",
                                            policyId,
                                            employee.getId()
                                    );

                                    return new ResourceNotFoundException(
                                            "The policy does not have a current published version"
                                    );
                                }
                        );

        PolicyAssignment assignment =
                assignmentRepository
                        .findByEmployee_IdAndPolicyVersion_Id(
                                employee.getId(),
                                currentVersion.getId()
                        )
                        .orElseThrow(
                                () -> {

                                    log.warn(
                                            "event=POLICY_ACKNOWLEDGEMENT_DENIED policyId={} versionId={} employeeId={} reason=POLICY_NOT_ASSIGNED",
                                            policyId,
                                            currentVersion.getId(),
                                            employee.getId()
                                    );

                                    return new ForbiddenException(
                                            "This policy is not assigned to your account"
                                    );
                                }
                        );

        LocalDateTime acknowledgedAt =
                LocalDateTime.now(
                        ZoneOffset.UTC
                );

        String ipAddress =
                resolveIpAddress(
                        request
                );

        String userAgent =
                limit(
                        request.getHeader(
                                "User-Agent"
                        ),
                        1000
                );

        PolicyAcknowledgement acknowledgement =
                acknowledgementRepository
                        .findByEmployee_IdAndPolicyVersion_Id(
                                employee.getId(),
                                currentVersion.getId()
                        )
                        .orElse(
                                null
                        );

        boolean updatedExisting =
                acknowledgement != null;

        if (acknowledgement == null) {

            acknowledgement =
                    PolicyAcknowledgement
                            .builder()
                            .employee(
                                    employee
                            )
                            .policy(
                                    policy
                            )
                            .policyVersion(
                                    currentVersion
                            )
                            .build();

            log.debug(
                    "event=POLICY_ACKNOWLEDGEMENT_RECORD_CREATE policyId={} versionId={} employeeId={}",
                    policyId,
                    currentVersion.getId(),
                    employee.getId()
            );

        } else {

            log.debug(
                    "event=POLICY_ACKNOWLEDGEMENT_RECORD_UPDATE policyId={} versionId={} employeeId={} acknowledgementId={}",
                    policyId,
                    currentVersion.getId(),
                    employee.getId(),
                    acknowledgement.getId()
            );
        }

        acknowledgement.setAcknowledgedAt(
                acknowledgedAt
        );

        acknowledgement.setIpAddress(
                ipAddress
        );

        acknowledgement.setUserAgent(
                userAgent
        );

        PolicyAcknowledgement saved =
                acknowledgementRepository.save(
                        acknowledgement
                );

        assignment.setStatus(
                AssignmentStatus.ACKNOWLEDGED
        );

        assignmentRepository.save(
                assignment
        );

        log.info(
                "event=POLICY_ACKNOWLEDGED policyId={} versionId={} versionNumber={} employeeId={} acknowledgementId={} updatedExisting={}",
                policy.getId(),
                currentVersion.getId(),
                currentVersion.getVersionNumber(),
                employee.getId(),
                saved.getId(),
                updatedExisting
        );

        return AcknowledgementResponse
                .builder()

                .acknowledgementId(
                        saved.getId()
                )

                .employeeId(
                        employee.getId()
                )

                .employeeName(
                        employee.getName()
                )

                .employeeEmail(
                        employee.getEmail()
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

                .policyVersionId(
                        currentVersion.getId()
                )

                .versionNumber(
                        currentVersion
                                .getVersionNumber()
                )

                .acknowledgedAt(
                        saved.getAcknowledgedAt()
                )

                .ipAddress(
                        saved.getIpAddress()
                )

                .userAgent(
                        saved.getUserAgent()
                )

                .updatedExistingAcknowledgement(
                        updatedExisting
                )

                .build();
    }


    // =========================================================
    // GET MY POLICY STATUS
    // =========================================================

    @Transactional(readOnly = true)
    public List<MyPolicyStatusResponse> getMyStatus() {

        User employee =
                currentEmployee();

        log.debug(
                "event=MY_POLICY_STATUS_REQUEST employeeId={}",
                employee.getId()
        );

        LocalDate today =
                LocalDate.now(
                        ZoneOffset.UTC
                );

        List<MyPolicyStatusResponse> results =
                assignmentRepository
                        .findByEmployee_IdAndPolicyVersion_CurrentVersionTrueOrderByDeadlineAsc(
                                employee.getId()
                        )
                        .stream()
                        .filter(
                                assignment ->
                                        assignment.getPolicy()
                                                .getStatus()
                                                == PolicyStatus.PUBLISHED
                        )
                        .map(
                                assignment ->
                                        mapStatus(
                                                assignment,
                                                employee,
                                                today
                                        )
                        )
                        .toList();

        log.debug(
                "event=MY_POLICY_STATUS_COMPLETED employeeId={} policyCount={}",
                employee.getId(),
                results.size()
        );

        return results;
    }


    // =========================================================
    // MAP POLICY STATUS
    // =========================================================

    private MyPolicyStatusResponse mapStatus(
            PolicyAssignment assignment,
            User employee,
            LocalDate today
    ) {

        Policy policy =
                assignment.getPolicy();

        PolicyVersion version =
                assignment.getPolicyVersion();

        PolicyAcknowledgement acknowledgement =
                acknowledgementRepository
                        .findByEmployee_IdAndPolicyVersion_Id(
                                employee.getId(),
                                version.getId()
                        )
                        .orElse(
                                null
                        );

        boolean acknowledged =
                acknowledgement != null;

        long daysOverdue =
                0;

        if (!acknowledged
                && today.isAfter(
                assignment.getDeadline()
        )) {

            daysOverdue =
                    ChronoUnit.DAYS.between(
                            assignment.getDeadline(),
                            today
                    );
        }

        AssignmentStatus displayStatus;

        if (acknowledged) {

            displayStatus =
                    AssignmentStatus.ACKNOWLEDGED;

        } else if (daysOverdue > 0) {

            displayStatus =
                    AssignmentStatus.OVERDUE;

        } else {

            displayStatus =
                    AssignmentStatus.PENDING;
        }

        log.debug(
                "event=POLICY_STATUS_RESOLVED policyId={} versionId={} employeeId={} status={} daysOverdue={}",
                policy.getId(),
                version.getId(),
                employee.getId(),
                displayStatus,
                daysOverdue
        );

        return MyPolicyStatusResponse
                .builder()

                .assignmentId(
                        assignment.getId()
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

                .categoryId(
                        policy.getCategory()
                                .getId()
                )

                .categoryName(
                        policy.getCategory()
                                .getName()
                )

                .mandatory(
                        policy.getMandatory()
                )

                .policyVersionId(
                        version.getId()
                )

                .versionNumber(
                        version.getVersionNumber()
                )

                .effectiveDate(
                        version.getEffectiveDate()
                )

                .deadline(
                        assignment.getDeadline()
                )

                .acknowledged(
                        acknowledged
                )

                .acknowledgedAt(
                        acknowledgement == null
                                ? null
                                : acknowledgement
                                .getAcknowledgedAt()
                )

                .daysOverdue(
                        daysOverdue
                )

                .assignmentStatus(
                        displayStatus
                )

                .build();
    }


    // =========================================================
    // GET CURRENT EMPLOYEE
    // =========================================================

    private User currentEmployee() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                ||
                !authentication
                        .isAuthenticated()) {

            log.warn(
                    "event=POLICY_ACKNOWLEDGEMENT_AUTH_FAILED reason=NO_AUTHENTICATED_USER"
            );

            throw new UnauthorizedException(
                    "Authentication is required"
            );
        }

        User user =
                userRepository
                        .findByEmailIgnoreCase(
                                authentication.getName()
                        )
                        .orElseThrow(
                                () -> {

                                    log.warn(
                                            "event=POLICY_ACKNOWLEDGEMENT_AUTH_FAILED reason=USER_NOT_FOUND"
                                    );

                                    return new UnauthorizedException(
                                            "Authenticated user was not found"
                                    );
                                }
                        );

        if (user.getRole()
                != Role.EMPLOYEE) {

            log.warn(
                    "event=POLICY_ACKNOWLEDGEMENT_ACCESS_DENIED userId={} role={} reason=NOT_EMPLOYEE",
                    user.getId(),
                    user.getRole()
            );

            throw new ForbiddenException(
                    "Only an Employee account can acknowledge policies"
            );
        }

        return user;
    }


    // =========================================================
    // RESOLVE IP ADDRESS
    // =========================================================

    private String resolveIpAddress(
            HttpServletRequest request
    ) {

        String forwarded =
                request.getHeader(
                        "X-Forwarded-For"
                );

        if (StringUtils.hasText(
                forwarded
        )) {

            return limit(
                    forwarded
                            .split(",")[0]
                            .trim(),
                    45
            );
        }

        return limit(
                request.getRemoteAddr(),
                45
        );
    }


    // =========================================================
    // LIMIT STRING LENGTH
    // =========================================================

    private String limit(
            String value,
            int maximumLength
    ) {

        if (value == null) {
            return null;
        }

        return value.length()
                <= maximumLength
                ? value
                : value.substring(
                0,
                maximumLength
        );
    }
}