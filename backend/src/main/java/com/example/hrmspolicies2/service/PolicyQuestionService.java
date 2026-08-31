package com.example.hrmspolicies2.service;

import com.example.hrmspolicies2.dto.AskPolicyRequest;
import com.example.hrmspolicies2.dto.response.*;
import com.example.hrmspolicies2.entity.Policy;
import com.example.hrmspolicies2.entity.PolicyVersion;
import com.example.hrmspolicies2.entity.User;
import com.example.hrmspolicies2.enums.*;
import com.example.hrmspolicies2.exception.UnauthorizedException;
import com.example.hrmspolicies2.repository.PolicyVersionRepository;
import com.example.hrmspolicies2.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PolicyQuestionService {

    private static final Set<String> STOP_WORDS =
            Set.of(
                    "a",
                    "an",
                    "and",
                    "are",
                    "can",
                    "do",
                    "for",
                    "how",
                    "i",
                    "in",
                    "is",
                    "it",
                    "my",
                    "of",
                    "on",
                    "or",
                    "the",
                    "to",
                    "what",
                    "when",
                    "where",
                    "who",
                    "why"
            );

    private final PolicyVersionRepository versionRepository;
    private final UserRepository userRepository;

    public PolicyQuestionService(
            PolicyVersionRepository versionRepository,
            UserRepository userRepository
    ) {
        this.versionRepository =
                versionRepository;

        this.userRepository =
                userRepository;
    }

    @Transactional(readOnly = true)
    public PolicyAnswerResponse ask(
            AskPolicyRequest request
    ) {
        User user = currentUser();

        Set<String> questionTerms =
                tokenize(
                        request.getQuestion()
                );

        List<ScoredVersion> matches =
                versionRepository
                        .findByCurrentVersionTrueAndPolicy_Status(
                                PolicyStatus.PUBLISHED
                        )
                        .stream()
                        .filter(version ->
                                canAccess(
                                        user,
                                        version.getPolicy()
                                )
                        )
                        .map(version ->
                                score(
                                        version,
                                        questionTerms
                                )
                        )
                        .filter(result ->
                                result.score() > 0
                        )
                        .sorted(
                                Comparator.comparingInt(
                                        ScoredVersion::score
                                ).reversed()
                        )
                        .limit(3)
                        .toList();

        if (matches.isEmpty()) {
            return PolicyAnswerResponse
                    .builder()
                    .question(
                            request.getQuestion()
                    )
                    .answer(
                            "I could not find a relevant answer "
                                    + "in the currently published policies."
                    )
                    .aiProviderConfigured(false)
                    .retrievalMode(
                            "LOCAL_KEYWORD_RETRIEVAL"
                    )
                    .sources(List.of())
                    .build();
        }

        List<PolicyAnswerSource> sources =
                matches.stream()
                        .map(this::mapSource)
                        .toList();

        String sourceNames =
                sources.stream()
                        .map(source ->
                                source.getPolicyCode()
                                        + " "
                                        + source.getPolicyName()
                        )
                        .collect(
                                Collectors.joining(
                                        "; "
                                )
                        );

        String answer =
                "The most relevant published policy information "
                        + "was found in: "
                        + sourceNames
                        + ". Review the cited excerpts below "
                        + "for the official policy wording.";

        return PolicyAnswerResponse
                .builder()
                .question(
                        request.getQuestion()
                )
                .answer(answer)
                .aiProviderConfigured(false)
                .retrievalMode(
                        "LOCAL_KEYWORD_RETRIEVAL"
                )
                .sources(sources)
                .build();
    }

    private ScoredVersion score(
            PolicyVersion version,
            Set<String> questionTerms
    ) {
        Policy policy =
                version.getPolicy();

        String searchableText =
                (
                        policy.getName()
                                + " "
                                + policy.getCode()
                                + " "
                                + version.getContentSnapshot()
                ).toLowerCase(
                        Locale.ROOT
                );

        int score = 0;

        for (String term :
                questionTerms) {
            if (searchableText.contains(
                    term
            )) {
                score++;
            }

            if (policy.getName()
                    .toLowerCase(
                            Locale.ROOT
                    )
                    .contains(term)) {
                score += 2;
            }
        }

        return new ScoredVersion(
                version,
                score
        );
    }

    private PolicyAnswerSource mapSource(
            ScoredVersion match
    ) {
        PolicyVersion version =
                match.version();

        Policy policy =
                version.getPolicy();

        return PolicyAnswerSource.builder()
                .policyId(policy.getId())
                .policyCode(policy.getCode())
                .policyName(policy.getName())
                .versionNumber(
                        version.getVersionNumber()
                )
                .sectionReference(
                        "Relevant content"
                )
                .relevantExcerpt(
                        createExcerpt(
                                version.getContentSnapshot()
                        )
                )
                .relevanceScore(
                        match.score()
                )
                .build();
    }

    private String createExcerpt(
            String content
    ) {
        if (!StringUtils.hasText(
                content
        )) {
            return "";
        }

        String normalized =
                content.replaceAll(
                        "\\s+",
                        " "
                ).trim();

        return normalized.length() <= 500
                ? normalized
                : normalized.substring(
                0,
                500
        ) + "...";
    }

    private Set<String> tokenize(
            String question
    ) {
        return Arrays.stream(
                        question.toLowerCase(
                                        Locale.ROOT
                                )
                                .replaceAll(
                                        "[^a-z0-9 ]",
                                        " "
                                )
                                .split("\\s+")
                )
                .filter(
                        StringUtils::hasText
                )
                .filter(term ->
                        !STOP_WORDS.contains(
                                term
                        )
                )
                .filter(term ->
                        term.length() > 1
                )
                .collect(
                        Collectors.toSet()
                );
    }

    private boolean canAccess(
            User user,
            Policy policy
    ) {
        if (user.getRole()
                != Role.EMPLOYEE) {
            return true;
        }

        return switch (
                policy.getApplicability()
                ) {
            case ALL -> true;

            case DEPT_BASED ->
                    containsValue(
                            policy.getApplicableDepartments(),
                            user.getDepartment()
                    );

            case GRADE_BASED ->
                    containsValue(
                            policy.getApplicableGrades(),
                            user.getGrade()
                    );
        };
    }

    private boolean containsValue(
            String values,
            String employeeValue
    ) {
        if (!StringUtils.hasText(values)
                || !StringUtils.hasText(
                employeeValue
        )) {
            return false;
        }

        return Arrays.stream(
                        values.split(",")
                )
                .map(String::trim)
                .anyMatch(value ->
                        value.equalsIgnoreCase(
                                employeeValue.trim()
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

    private record ScoredVersion(
            PolicyVersion version,
            int score
    ) {
    }
}