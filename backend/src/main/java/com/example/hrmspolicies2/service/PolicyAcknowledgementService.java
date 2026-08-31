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

    @Transactional
    public AcknowledgementResponse acknowledge(
            Long policyId,
            HttpServletRequest request
    ) {
        User employee = currentEmployee();

        Policy policy = policyRepository
                .findById(policyId)
                .orElseThrow(() ->
                        ResourceNotFoundException
                                .forEntity(
                                        "Policy",
                                        policyId
                                )
                );

        if (policy.getStatus()
                != PolicyStatus.PUBLISHED) {
            throw new BadRequestException(
                    "Only a currently published policy can be acknowledged"
            );
        }

        PolicyVersion currentVersion =
                versionRepository
                        .findByPolicy_IdAndCurrentVersionTrue(
                                policyId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "The policy does not have a current published version"
                                )
                        );

        PolicyAssignment assignment =
                assignmentRepository
                        .findByEmployee_IdAndPolicyVersion_Id(
                                employee.getId(),
                                currentVersion.getId()
                        )
                        .orElseThrow(() ->
                                new ForbiddenException(
                                        "This policy is not assigned to your account"
                                )
                        );

        LocalDateTime acknowledgedAt =
                LocalDateTime.now(
                        ZoneOffset.UTC
                );

        String ipAddress =
                resolveIpAddress(request);

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
                        .orElse(null);

        boolean updatedExisting =
                acknowledgement != null;

        if (acknowledgement == null) {
            acknowledgement =
                    PolicyAcknowledgement.builder()
                            .employee(employee)
                            .policy(policy)
                            .policyVersion(
                                    currentVersion
                            )
                            .build();
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

    @Transactional(readOnly = true)
    public List<MyPolicyStatusResponse> getMyStatus() {
        User employee = currentEmployee();

        LocalDate today =
                LocalDate.now(
                        ZoneOffset.UTC
                );

        return assignmentRepository
                .findByEmployee_IdAndPolicyVersion_CurrentVersionTrueOrderByDeadlineAsc(
                        employee.getId()
                )
                .stream()
                .filter(assignment ->
                        assignment.getPolicy()
                                .getStatus()
                                == PolicyStatus.PUBLISHED
                )
                .map(assignment ->
                        mapStatus(
                                assignment,
                                employee,
                                today
                        )
                )
                .toList();
    }

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
                        .orElse(null);

        boolean acknowledged =
                acknowledgement != null;

        long daysOverdue = 0;

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

    private User currentEmployee() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication
                .isAuthenticated()) {
            throw new UnauthorizedException(
                    "Authentication is required"
            );
        }

        User user =
                userRepository
                        .findByEmailIgnoreCase(
                                authentication.getName()
                        )
                        .orElseThrow(() ->
                                new UnauthorizedException(
                                        "Authenticated user was not found"
                                )
                        );

        if (user.getRole()
                != Role.EMPLOYEE) {
            throw new ForbiddenException(
                    "Only an Employee account can acknowledge policies"
            );
        }

        return user;
    }

    private String resolveIpAddress(
            HttpServletRequest request
    ) {
        String forwarded =
                request.getHeader(
                        "X-Forwarded-For"
                );

        if (StringUtils.hasText(forwarded)) {
            return limit(
                    forwarded.split(",")[0]
                            .trim(),
                    45
            );
        }

        return limit(
                request.getRemoteAddr(),
                45
        );
    }

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