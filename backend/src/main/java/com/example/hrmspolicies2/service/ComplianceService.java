package com.example.hrmspolicies2.service;

import com.example.hrmspolicies2.dto.response.*;
import com.example.hrmspolicies2.entity.*;
import com.example.hrmspolicies2.enums.Applicability;
import com.example.hrmspolicies2.enums.PolicyStatus;
import com.example.hrmspolicies2.enums.Role;
import com.example.hrmspolicies2.exception.BadRequestException;
import com.example.hrmspolicies2.repository.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ComplianceService {

    // =========================================================
    // STRUCTURED LOGGING
    // =========================================================

    private static final Logger log =
            LoggerFactory.getLogger(
                    ComplianceService.class
            );

    private static final Set<String>
            SORTABLE_FIELDS = Set.of(
            "policyName",
            "policyCode",
            "effectiveDate",
            "completionPercentage",
            "totalApplicableEmployees",
            "acknowledgedEmployees",
            "pendingEmployees",
            "overdueEmployees"
    );

    private final PolicyRepository policyRepository;
    private final PolicyVersionRepository versionRepository;
    private final PolicyAssignmentRepository assignmentRepository;
    private final PolicyAcknowledgementRepository acknowledgementRepository;
    private final UserRepository userRepository;


    public ComplianceService(
            PolicyRepository policyRepository,
            PolicyVersionRepository versionRepository,
            PolicyAssignmentRepository assignmentRepository,
            PolicyAcknowledgementRepository acknowledgementRepository,
            UserRepository userRepository
    ) {
        this.policyRepository =
                policyRepository;

        this.versionRepository =
                versionRepository;

        this.assignmentRepository =
                assignmentRepository;

        this.acknowledgementRepository =
                acknowledgementRepository;

        this.userRepository =
                userRepository;
    }


    // =========================================================
    // COMPLIANCE DASHBOARD
    // =========================================================

    @Transactional(readOnly = true)
    public PageResponse<PolicyComplianceResponse>
    getComplianceDashboard(
            String search,
            Long policyId,
            Long categoryId,
            String department,
            Boolean mandatory,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size,
            String sortBy,
            String direction
    ) {

        log.debug(
                "event=COMPLIANCE_DASHBOARD_REQUEST policyId={} categoryId={} departmentPresent={} mandatory={} fromDate={} toDate={} page={} size={} sortBy={} direction={} searchPresent={}",
                policyId,
                categoryId,
                StringUtils.hasText(department),
                mandatory,
                fromDate,
                toDate,
                page,
                size,
                sortBy,
                direction,
                StringUtils.hasText(search)
        );

        validateDates(
                fromDate,
                toDate
        );


        String safeSortBy =
                StringUtils.hasText(
                        sortBy
                )
                        ? sortBy
                        : "policyName";


        if (!SORTABLE_FIELDS.contains(
                safeSortBy
        )) {

            log.warn(
                    "event=COMPLIANCE_DASHBOARD_REJECTED reason=INVALID_SORT_FIELD sortBy={}",
                    safeSortBy
            );

            throw new BadRequestException(
                    "Invalid compliance sorting field: "
                            + safeSortBy
            );
        }


        List<User> employees =
                userRepository.findByRole(
                        Role.EMPLOYEE
                );


        log.debug(
                "event=COMPLIANCE_EMPLOYEES_LOADED employeeCount={}",
                employees.size()
        );


        List<PolicyComplianceResponse> results =
                policyRepository
                        .findByStatus(
                                PolicyStatus.PUBLISHED
                        )
                        .stream()
                        .filter(
                                policy ->
                                        matchesPolicyFilters(
                                                policy,
                                                search,
                                                policyId,
                                                categoryId,
                                                mandatory,
                                                fromDate,
                                                toDate
                                        )
                        )
                        .map(
                                policy ->
                                        calculatePolicyCompliance(
                                                policy,
                                                employees,
                                                department
                                        )
                        )
                        .filter(
                                Objects::nonNull
                        )
                        .sorted(
                                complianceComparator(
                                        safeSortBy,
                                        direction
                                )
                        )
                        .toList();


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


        int start =
                Math.min(
                        safePage
                                * safeSize,
                        results.size()
                );


        int end =
                Math.min(
                        start
                                + safeSize,
                        results.size()
                );


        PageImpl<PolicyComplianceResponse>
                resultPage =
                new PageImpl<>(
                        results.subList(
                                start,
                                end
                        ),
                        org.springframework.data.domain
                                .PageRequest.of(
                                        safePage,
                                        safeSize
                                ),
                        results.size()
                );


        log.debug(
                "event=COMPLIANCE_DASHBOARD_COMPLETED totalPolicies={} returnedPolicies={} page={} size={}",
                results.size(),
                resultPage.getNumberOfElements(),
                safePage,
                safeSize
        );


        return new PageResponse<>(
                resultPage
        );
    }


    // =========================================================
    // EXPORT COMPLIANCE CSV
    // =========================================================

    @Transactional(readOnly = true)
    public byte[] exportCsv(
            String search,
            Long policyId,
            Long categoryId,
            String department,
            Boolean mandatory,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        log.info(
                "event=COMPLIANCE_CSV_EXPORT_REQUEST policyId={} categoryId={} departmentPresent={} mandatory={} fromDate={} toDate={} searchPresent={}",
                policyId,
                categoryId,
                StringUtils.hasText(department),
                mandatory,
                fromDate,
                toDate,
                StringUtils.hasText(search)
        );


        PageResponse<PolicyComplianceResponse> result =
                getComplianceDashboard(
                        search,
                        policyId,
                        categoryId,
                        department,
                        mandatory,
                        fromDate,
                        toDate,
                        0,
                        100,
                        "policyName",
                        "asc"
                );


        StringBuilder csv =
                new StringBuilder();


        csv.append(
                "Policy Code,Policy Name,Category,"
                        + "Version,Effective Date,Mandatory,"
                        + "Applicable Employees,Acknowledged,"
                        + "Pending,Overdue,Completion Percentage\n"
        );


        for (PolicyComplianceResponse row :
                result.getContent()) {

            csv.append(
                    escapeCsv(
                            row.getPolicyCode()
                    )
            ).append(",");

            csv.append(
                    escapeCsv(
                            row.getPolicyName()
                    )
            ).append(",");

            csv.append(
                    escapeCsv(
                            row.getCategoryName()
                    )
            ).append(",");

            csv.append(
                    row.getCurrentVersion()
            ).append(",");

            csv.append(
                    row.getEffectiveDate()
            ).append(",");

            csv.append(
                    row.getMandatory()
            ).append(",");

            csv.append(
                    row.getTotalApplicableEmployees()
            ).append(",");

            csv.append(
                    row.getAcknowledgedEmployees()
            ).append(",");

            csv.append(
                    row.getPendingEmployees()
            ).append(",");

            csv.append(
                    row.getOverdueEmployees()
            ).append(",");

            csv.append(
                    String.format(
                            Locale.US,
                            "%.2f",
                            row.getCompletionPercentage()
                    )
            ).append("\n");
        }


        byte[] csvBytes =
                csv.toString()
                        .getBytes(
                                StandardCharsets.UTF_8
                        );


        log.info(
                "event=COMPLIANCE_CSV_EXPORT_COMPLETED rowCount={} byteSize={}",
                result.getContent()
                        .size(),
                csvBytes.length
        );


        return csvBytes;
    }


    // =========================================================
    // CALCULATE POLICY COMPLIANCE
    // =========================================================

    private PolicyComplianceResponse
    calculatePolicyCompliance(
            Policy policy,
            List<User> allEmployees,
            String departmentFilter
    ) {

        PolicyVersion currentVersion =
                versionRepository
                        .findByPolicy_IdAndCurrentVersionTrue(
                                policy.getId()
                        )
                        .orElse(
                                null
                        );


        if (currentVersion == null) {

            log.debug(
                    "event=COMPLIANCE_POLICY_SKIPPED policyId={} reason=CURRENT_VERSION_NOT_FOUND",
                    policy.getId()
            );

            return null;
        }


        List<User> applicableEmployees =
                allEmployees
                        .stream()
                        .filter(
                                employee ->
                                        isApplicable(
                                                policy,
                                                employee
                                        )
                        )
                        .filter(
                                employee ->
                                        !StringUtils.hasText(
                                                departmentFilter
                                        )
                                                ||
                                                departmentFilter
                                                        .equalsIgnoreCase(
                                                                employee.getDepartment()
                                                        )
                        )
                        .toList();


        LocalDate today =
                LocalDate.now(
                        ZoneOffset.UTC
                );


        long acknowledgedCount =
                0;

        long overdueCount =
                0;


        List<OverdueEmployeeResponse>
                overdueEmployees =
                new ArrayList<>();


        Map<String, List<User>>
                employeesByDepartment =
                applicableEmployees
                        .stream()
                        .collect(
                                Collectors.groupingBy(
                                        employee ->
                                                StringUtils.hasText(
                                                        employee.getDepartment()
                                                )
                                                        ? employee
                                                        .getDepartment()
                                                        : "Unassigned"
                                )
                        );


        for (User employee :
                applicableEmployees) {

            boolean acknowledged =
                    acknowledgementRepository
                            .existsByEmployee_IdAndPolicyVersion_Id(
                                    employee.getId(),
                                    currentVersion.getId()
                            );


            if (acknowledged) {

                acknowledgedCount++;

                continue;
            }


            PolicyAssignment assignment =
                    assignmentRepository
                            .findByEmployee_IdAndPolicyVersion_Id(
                                    employee.getId(),
                                    currentVersion.getId()
                            )
                            .orElse(
                                    null
                            );


            if (assignment != null
                    &&
                    today.isAfter(
                            assignment.getDeadline()
                    )) {

                overdueCount++;


                overdueEmployees.add(
                        OverdueEmployeeResponse
                                .builder()

                                .employeeId(
                                        employee.getId()
                                )

                                .employeeName(
                                        employee.getName()
                                )

                                .employeeEmail(
                                        employee.getEmail()
                                )

                                .department(
                                        employee.getDepartment()
                                )

                                .deadline(
                                        assignment.getDeadline()
                                )

                                .daysOverdue(
                                        ChronoUnit.DAYS
                                                .between(
                                                        assignment.getDeadline(),
                                                        today
                                                )
                                )

                                .build()
                );
            }
        }


        long totalApplicable =
                applicableEmployees.size();


        long pending =
                totalApplicable
                        - acknowledgedCount;


        double percentage =
                totalApplicable == 0
                        ? 0.0
                        : acknowledgedCount
                        * 100.0
                        / totalApplicable;


        List<DepartmentComplianceResponse>
                departmentBreakdown =
                employeesByDepartment
                        .entrySet()
                        .stream()
                        .map(
                                entry ->
                                        calculateDepartment(
                                                entry.getKey(),
                                                entry.getValue(),
                                                currentVersion,
                                                today
                                        )
                        )
                        .sorted(
                                Comparator.comparing(
                                        DepartmentComplianceResponse
                                                ::getDepartment,
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        )
                        .toList();


        overdueEmployees.sort(
                Comparator.comparing(
                        OverdueEmployeeResponse
                                ::getDaysOverdue
                ).reversed()
        );


        log.debug(
                "event=COMPLIANCE_POLICY_CALCULATED policyId={} versionId={} totalApplicable={} acknowledged={} pending={} overdue={} completionPercentage={}",
                policy.getId(),
                currentVersion.getId(),
                totalApplicable,
                acknowledgedCount,
                pending,
                overdueCount,
                roundPercentage(
                        percentage
                )
        );


        return PolicyComplianceResponse
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
                        currentVersion.getId()
                )

                .currentVersion(
                        currentVersion
                                .getVersionNumber()
                )

                .effectiveDate(
                        currentVersion
                                .getEffectiveDate()
                )

                .totalApplicableEmployees(
                        totalApplicable
                )

                .acknowledgedEmployees(
                        acknowledgedCount
                )

                .pendingEmployees(
                        pending
                )

                .overdueEmployees(
                        overdueCount
                )

                .completionPercentage(
                        roundPercentage(
                                percentage
                        )
                )

                .departmentBreakdown(
                        departmentBreakdown
                )

                .overdueEmployeeList(
                        overdueEmployees
                )

                .build();
    }


    // =========================================================
    // CALCULATE DEPARTMENT COMPLIANCE
    // =========================================================

    private DepartmentComplianceResponse
    calculateDepartment(
            String department,
            List<User> employees,
            PolicyVersion version,
            LocalDate today
    ) {

        long acknowledged =
                0;

        long overdue =
                0;


        for (User employee :
                employees) {

            boolean hasAcknowledged =
                    acknowledgementRepository
                            .existsByEmployee_IdAndPolicyVersion_Id(
                                    employee.getId(),
                                    version.getId()
                            );


            if (hasAcknowledged) {

                acknowledged++;

                continue;
            }


            PolicyAssignment assignment =
                    assignmentRepository
                            .findByEmployee_IdAndPolicyVersion_Id(
                                    employee.getId(),
                                    version.getId()
                            )
                            .orElse(
                                    null
                            );


            if (assignment != null
                    &&
                    today.isAfter(
                            assignment.getDeadline()
                    )) {

                overdue++;
            }
        }


        long total =
                employees.size();


        long pending =
                total
                        - acknowledged;


        double percentage =
                total == 0
                        ? 0.0
                        : acknowledged
                        * 100.0
                        / total;


        return DepartmentComplianceResponse
                .builder()

                .department(
                        department
                )

                .totalApplicableEmployees(
                        total
                )

                .acknowledgedEmployees(
                        acknowledged
                )

                .pendingEmployees(
                        pending
                )

                .overdueEmployees(
                        overdue
                )

                .completionPercentage(
                        roundPercentage(
                                percentage
                        )
                )

                .build();
    }


    // =========================================================
    // POLICY FILTERS
    // =========================================================

    private boolean matchesPolicyFilters(
            Policy policy,
            String search,
            Long policyId,
            Long categoryId,
            Boolean mandatory,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        if (policyId != null
                &&
                !policy.getId()
                        .equals(
                                policyId
                        )) {

            return false;
        }


        if (categoryId != null
                &&
                !policy.getCategory()
                        .getId()
                        .equals(
                                categoryId
                        )) {

            return false;
        }


        if (mandatory != null
                &&
                !policy.getMandatory()
                        .equals(
                                mandatory
                        )) {

            return false;
        }


        if (StringUtils.hasText(
                search
        )) {

            String keyword =
                    search.trim()
                            .toLowerCase(
                                    Locale.ROOT
                            );


            boolean matches =
                    policy.getName()
                            .toLowerCase(
                                    Locale.ROOT
                            )
                            .contains(
                                    keyword
                            )
                            ||
                            policy.getCode()
                                    .toLowerCase(
                                            Locale.ROOT
                                    )
                                    .contains(
                                            keyword
                                    );


            if (!matches) {

                return false;
            }
        }


        if (fromDate != null
                &&
                (
                        policy.getEffectiveDate()
                                == null
                                ||
                                policy.getEffectiveDate()
                                        .isBefore(
                                                fromDate
                                        )
                )) {

            return false;
        }


        return toDate == null
                ||
                (
                        policy.getEffectiveDate()
                                != null
                                &&
                                !policy.getEffectiveDate()
                                        .isAfter(
                                                toDate
                                        )
                );
    }


    // =========================================================
    // POLICY APPLICABILITY
    // =========================================================

    private boolean isApplicable(
            Policy policy,
            User employee
    ) {

        if (policy.getApplicability()
                == Applicability.ALL) {

            return true;
        }


        if (policy.getApplicability()
                == Applicability.DEPT_BASED) {

            return containsValue(
                    policy.getApplicableDepartments(),
                    employee.getDepartment()
            );
        }


        return containsValue(
                policy.getApplicableGrades(),
                employee.getGrade()
        );
    }


    // =========================================================
    // VALUE MATCHING
    // =========================================================

    private boolean containsValue(
            String values,
            String employeeValue
    ) {

        if (!StringUtils.hasText(
                values
        )
                ||
                !StringUtils.hasText(
                        employeeValue
                )) {

            return false;
        }


        return Arrays.stream(
                        values.split(",")
                )
                .map(
                        String::trim
                )
                .anyMatch(
                        value ->
                                value.equalsIgnoreCase(
                                        employeeValue.trim()
                                )
                );
    }


    // =========================================================
    // COMPLIANCE SORTING
    // =========================================================

    private Comparator<PolicyComplianceResponse>
    complianceComparator(
            String sortBy,
            String direction
    ) {

        Function<
                PolicyComplianceResponse,
                Comparable<?>
                >
                extractor =
                switch (sortBy) {

                    case "policyCode" ->
                            PolicyComplianceResponse
                                    ::getPolicyCode;

                    case "effectiveDate" ->
                            response ->
                                    response.getEffectiveDate()
                                            == null
                                            ? LocalDate.MIN
                                            : response
                                            .getEffectiveDate();

                    case "completionPercentage" ->
                            PolicyComplianceResponse
                                    ::getCompletionPercentage;

                    case "totalApplicableEmployees" ->
                            PolicyComplianceResponse
                                    ::getTotalApplicableEmployees;

                    case "acknowledgedEmployees" ->
                            PolicyComplianceResponse
                                    ::getAcknowledgedEmployees;

                    case "pendingEmployees" ->
                            PolicyComplianceResponse
                                    ::getPendingEmployees;

                    case "overdueEmployees" ->
                            PolicyComplianceResponse
                                    ::getOverdueEmployees;

                    default ->
                            PolicyComplianceResponse
                                    ::getPolicyName;
                };


        Comparator<PolicyComplianceResponse>
                comparator =
                (first, second) ->
                        compareComparable(
                                extractor.apply(
                                        first
                                ),
                                extractor.apply(
                                        second
                                )
                        );


        if ("desc".equalsIgnoreCase(
                direction
        )) {

            comparator =
                    comparator.reversed();
        }


        return comparator;
    }


    // =========================================================
    // COMPARABLE HELPER
    // =========================================================

    @SuppressWarnings({
            "rawtypes",
            "unchecked"
    })
    private int compareComparable(
            Comparable first,
            Comparable second
    ) {

        if (first == null
                &&
                second == null) {

            return 0;
        }


        if (first == null) {

            return -1;
        }


        if (second == null) {

            return 1;
        }


        return first.compareTo(
                second
        );
    }


    // =========================================================
    // ROUND PERCENTAGE
    // =========================================================

    private double roundPercentage(
            double percentage
    ) {

        return Math.round(
                percentage
                        * 100.0
        ) / 100.0;
    }


    // =========================================================
    // VALIDATE DATE RANGE
    // =========================================================

    private void validateDates(
            LocalDate fromDate,
            LocalDate toDate
    ) {

        if (fromDate != null
                &&
                toDate != null
                &&
                fromDate.isAfter(
                        toDate
                )) {

            log.warn(
                    "event=COMPLIANCE_REQUEST_REJECTED reason=INVALID_DATE_RANGE fromDate={} toDate={}",
                    fromDate,
                    toDate
            );

            throw new BadRequestException(
                    "fromDate cannot be after toDate"
            );
        }
    }


    // =========================================================
    // ESCAPE CSV VALUE
    // =========================================================

    private String escapeCsv(
            Object value
    ) {

        if (value == null) {

            return "";
        }


        String text =
                String.valueOf(
                        value
                );


        return "\""
                + text.replace(
                "\"",
                "\"\""
        )
                + "\"";
    }
}