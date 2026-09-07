package com.example.hrmspolicies2.service;

import com.example.hrmspolicies2.dto.PolicyRequest;
import com.example.hrmspolicies2.dto.response.PageResponse;
import com.example.hrmspolicies2.dto.response.PolicyResponse;
import com.example.hrmspolicies2.entity.Policy;
import com.example.hrmspolicies2.entity.PolicyCategory;
import com.example.hrmspolicies2.entity.User;
import com.example.hrmspolicies2.enums.Applicability;
import com.example.hrmspolicies2.enums.PolicyStatus;
import com.example.hrmspolicies2.enums.Role;
import com.example.hrmspolicies2.exception.BadRequestException;
import com.example.hrmspolicies2.exception.DuplicateResourceException;
import com.example.hrmspolicies2.exception.ResourceNotFoundException;
import com.example.hrmspolicies2.repository.PolicyCategoryRepository;
import com.example.hrmspolicies2.repository.PolicyRepository;
import com.example.hrmspolicies2.repository.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PolicyService Unit Tests")
class PolicyServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private PolicyCategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PolicyService policyService;

    private PolicyCategory category;
    private Policy samplePolicy;
    private PolicyRequest sampleRequest;
    private User hrAdmin;


    // =========================================================
    // COMMON TEST DATA
    // =========================================================

    @BeforeEach
    void setUp() {

        SecurityContextHolder.clearContext();

        category =
                mock(
                        PolicyCategory.class
                );

        when(
                category.getId()
        ).thenReturn(
                1L
        );

        when(
                category.getName()
        ).thenReturn(
                "HR"
        );

        when(
                category.getCode()
        ).thenReturn(
                "HR"
        );

        when(
                category.getActive()
        ).thenReturn(
                true
        );


        hrAdmin =
                User.builder()
                        .id(
                                10L
                        )
                        .name(
                                "HR Admin"
                        )
                        .email(
                                "admin@example.com"
                        )
                        .role(
                                Role.HR_ADMIN
                        )
                        .build();


        samplePolicy =
                Policy.builder()
                        .id(
                                1L
                        )
                        .name(
                                "Work From Home Policy"
                        )
                        .code(
                                "WFH-001"
                        )
                        .category(
                                category
                        )
                        .content(
                                "Employees may work remotely up to 3 days per week."
                        )
                        .applicability(
                                Applicability.ALL
                        )
                        .mandatory(
                                true
                        )
                        .status(
                                PolicyStatus.DRAFT
                        )
                        .acknowledgementPeriodDays(
                                7
                        )
                        .onboardingPeriodDays(
                                7
                        )
                        .publishedOnce(
                                false
                        )
                        .createdBy(
                                hrAdmin
                        )
                        .build();


        sampleRequest =
                new PolicyRequest();

        sampleRequest.setName(
                "Work From Home Policy"
        );

        sampleRequest.setCode(
                "WFH-001"
        );

        sampleRequest.setCategoryId(
                1L
        );

        sampleRequest.setContent(
                "Employees may work remotely up to 3 days per week."
        );

        sampleRequest.setApplicability(
                Applicability.ALL
        );

        sampleRequest.setMandatory(
                true
        );

        sampleRequest.setAcknowledgementPeriodDays(
                7
        );

        sampleRequest.setOnboardingPeriodDays(
                7
        );


        /*
         * Simulate authenticated HR Admin.
         */
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "admin@example.com",
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_HR_ADMIN"
                                )
                        )
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(
                        authentication
                );
    }


    @AfterEach
    void tearDown() {

        SecurityContextHolder.clearContext();
    }


    // =========================================================
    // SEARCH TESTS
    // =========================================================

    @Nested
    @DisplayName("searchPolicies()")
    class SearchTests {

        @Test
        @DisplayName(
                "returns matching policy responses"
        )
        void searchPolicies_success() {

            Page<Policy> page =
                    new PageImpl<>(
                            List.of(
                                    samplePolicy
                            )
                    );


            when(
                    policyRepository.findAll(
                            any(Specification.class),
                            any(Pageable.class)
                    )
            ).thenReturn(
                    page
            );


            PageResponse<PolicyResponse> result =
                    policyService.searchPolicies(
                            "home",
                            1L,
                            PolicyStatus.DRAFT,
                            Applicability.ALL,
                            true,
                            0,
                            10,
                            "name",
                            "asc"
                    );


            assertThat(
                    result.getContent()
            ).hasSize(
                    1
            );


            assertThat(
                    result.getContent()
                            .get(0)
                            .getCode()
            ).isEqualTo(
                    "WFH-001"
            );
        }


        @Test
        @DisplayName(
                "invalid sorting field throws BadRequestException"
        )
        void searchPolicies_invalidSortField() {

            assertThatThrownBy(
                    () ->
                            policyService.searchPolicies(
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    0,
                                    10,
                                    "invalidField",
                                    "asc"
                            )
            )
                    .isInstanceOf(
                            BadRequestException.class
                    )
                    .hasMessageContaining(
                            "Invalid sorting field"
                    );


            verify(
                    policyRepository,
                    never()
            ).findAll(
                    any(Specification.class),
                    any(Pageable.class)
            );
        }


        @Test
        @DisplayName(
                "negative page number is safely converted to zero"
        )
        void searchPolicies_negativePage() {

            ArgumentCaptor<Pageable> pageableCaptor =
                    ArgumentCaptor.forClass(
                            Pageable.class
                    );


            when(
                    policyRepository.findAll(
                            any(Specification.class),
                            pageableCaptor.capture()
                    )
            ).thenReturn(
                    new PageImpl<>(
                            List.of(
                                    samplePolicy
                            )
                    )
            );


            policyService.searchPolicies(
                    null,
                    null,
                    null,
                    null,
                    null,
                    -5,
                    10,
                    "id",
                    "asc"
            );


            assertThat(
                    pageableCaptor
                            .getValue()
                            .getPageNumber()
            ).isEqualTo(
                    0
            );
        }
    }


    // =========================================================
    // GET POLICY BY ID
    // =========================================================

    @Nested
    @DisplayName("getPolicyById()")
    class GetByIdTests {

        @Test
        @DisplayName(
                "returns policy response when policy exists"
        )
        void getPolicyById_success() {

            when(
                    policyRepository.findById(
                            1L
                    )
            ).thenReturn(
                    Optional.of(
                            samplePolicy
                    )
            );


            PolicyResponse result =
                    policyService.getPolicyById(
                            1L
                    );


            assertThat(
                    result.getId()
            ).isEqualTo(
                    1L
            );


            assertThat(
                    result.getCode()
            ).isEqualTo(
                    "WFH-001"
            );


            assertThat(
                    result.getStatus()
            ).isEqualTo(
                    PolicyStatus.DRAFT
            );
        }


        @Test
        @DisplayName(
                "throws ResourceNotFoundException when policy does not exist"
        )
        void getPolicyById_notFound() {

            when(
                    policyRepository.findById(
                            99L
                    )
            ).thenReturn(
                    Optional.empty()
            );


            assertThatThrownBy(
                    () ->
                            policyService.getPolicyById(
                                    99L
                            )
            )
                    .isInstanceOf(
                            ResourceNotFoundException.class
                    );
        }
    }


    // =========================================================
    // CREATE POLICY
    // =========================================================

    @Nested
    @DisplayName("createPolicy()")
    class CreateTests {

        @Test
        @DisplayName(
                "creates policy successfully"
        )
        void createPolicy_success() {

            when(
                    policyRepository.existsByCodeIgnoreCase(
                            "WFH-001"
                    )
            ).thenReturn(
                    false
            );


            when(
                    categoryRepository.findById(
                            1L
                    )
            ).thenReturn(
                    Optional.of(
                            category
                    )
            );


            when(
                    userRepository.findByEmailIgnoreCase(
                            "admin@example.com"
                    )
            ).thenReturn(
                    Optional.of(
                            hrAdmin
                    )
            );


            when(
                    policyRepository.save(
                            any(Policy.class)
                    )
            ).thenAnswer(
                    invocation ->
                            invocation.getArgument(
                                    0
                            )
            );


            PolicyResponse result =
                    policyService.createPolicy(
                            sampleRequest
                    );


            assertThat(
                    result.getName()
            ).isEqualTo(
                    "Work From Home Policy"
            );


            assertThat(
                    result.getCode()
            ).isEqualTo(
                    "WFH-001"
            );


            assertThat(
                    result.getCategoryId()
            ).isEqualTo(
                    1L
            );


            assertThat(
                    result.getApplicability()
            ).isEqualTo(
                    Applicability.ALL
            );


            assertThat(
                    result.getStatus()
            ).isEqualTo(
                    PolicyStatus.DRAFT
            );


            verify(
                    policyRepository
            ).save(
                    any(Policy.class)
            );
        }


        @Test
        @DisplayName(
                "duplicate policy code throws DuplicateResourceException"
        )
        void createPolicy_duplicateCode() {

            when(
                    policyRepository.existsByCodeIgnoreCase(
                            "WFH-001"
                    )
            ).thenReturn(
                    true
            );


            assertThatThrownBy(
                    () ->
                            policyService.createPolicy(
                                    sampleRequest
                            )
            )
                    .isInstanceOf(
                            DuplicateResourceException.class
                    )
                    .hasMessage(
                            "Policy code already exists: WFH-001"
                    );


            verify(
                    policyRepository,
                    never()
            ).save(
                    any(Policy.class)
            );
        }
    }


    // =========================================================
    // UPDATE POLICY
    // =========================================================

    @Nested
    @DisplayName("updatePolicy()")
    class UpdateTests {

        @Test
        @DisplayName(
                "updates draft policy successfully"
        )
        void updatePolicy_success() {

            when(
                    policyRepository.findById(
                            1L
                    )
            ).thenReturn(
                    Optional.of(
                            samplePolicy
                    )
            );


            when(
                    policyRepository
                            .existsByCodeIgnoreCaseAndIdNot(
                                    "WFH-001",
                                    1L
                            )
            ).thenReturn(
                    false
            );


            when(
                    categoryRepository.findById(
                            1L
                    )
            ).thenReturn(
                    Optional.of(
                            category
                    )
            );


            when(
                    policyRepository.save(
                            any(Policy.class)
                    )
            ).thenAnswer(
                    invocation ->
                            invocation.getArgument(
                                    0
                            )
            );


            sampleRequest.setName(
                    "Updated Work From Home Policy"
            );


            PolicyResponse result =
                    policyService.updatePolicy(
                            1L,
                            sampleRequest
                    );


            assertThat(
                    result.getName()
            ).isEqualTo(
                    "Updated Work From Home Policy"
            );


            assertThat(
                    result.getStatus()
            ).isEqualTo(
                    PolicyStatus.DRAFT
            );
        }


        @Test
        @DisplayName(
                "throws ResourceNotFoundException when updating missing policy"
        )
        void updatePolicy_notFound() {

            when(
                    policyRepository.findById(
                            99L
                    )
            ).thenReturn(
                    Optional.empty()
            );


            assertThatThrownBy(
                    () ->
                            policyService.updatePolicy(
                                    99L,
                                    sampleRequest
                            )
            )
                    .isInstanceOf(
                            ResourceNotFoundException.class
                    );


            verify(
                    policyRepository,
                    never()
            ).save(
                    any(Policy.class)
            );
        }


        @Test
        @DisplayName(
                "duplicate updated code throws DuplicateResourceException"
        )
        void updatePolicy_duplicateCode() {

            when(
                    policyRepository.findById(
                            1L
                    )
            ).thenReturn(
                    Optional.of(
                            samplePolicy
                    )
            );


            sampleRequest.setCode(
                    "WFH-002"
            );


            when(
                    policyRepository
                            .existsByCodeIgnoreCaseAndIdNot(
                                    "WFH-002",
                                    1L
                            )
            ).thenReturn(
                    true
            );


            assertThatThrownBy(
                    () ->
                            policyService.updatePolicy(
                                    1L,
                                    sampleRequest
                            )
            )
                    .isInstanceOf(
                            DuplicateResourceException.class
                    )
                    .hasMessage(
                            "Policy code already exists: WFH-002"
                    );


            verify(
                    policyRepository,
                    never()
            ).save(
                    any(Policy.class)
            );
        }


        @Test
        @DisplayName(
                "editing rejected policy returns it to DRAFT"
        )
        void updatePolicy_rejectedBecomesDraft() {

            samplePolicy.setStatus(
                    PolicyStatus.REJECTED
            );


            when(
                    policyRepository.findById(
                            1L
                    )
            ).thenReturn(
                    Optional.of(
                            samplePolicy
                    )
            );


            when(
                    policyRepository
                            .existsByCodeIgnoreCaseAndIdNot(
                                    "WFH-001",
                                    1L
                            )
            ).thenReturn(
                    false
            );


            when(
                    categoryRepository.findById(
                            1L
                    )
            ).thenReturn(
                    Optional.of(
                            category
                    )
            );


            when(
                    policyRepository.save(
                            any(Policy.class)
                    )
            ).thenAnswer(
                    invocation ->
                            invocation.getArgument(
                                    0
                            )
            );


            PolicyResponse result =
                    policyService.updatePolicy(
                            1L,
                            sampleRequest
                    );


            assertThat(
                    result.getStatus()
            ).isEqualTo(
                    PolicyStatus.DRAFT
            );
        }
    }


    // =========================================================
    // DELETE POLICY
    // =========================================================

    @Nested
    @DisplayName("deletePolicy()")
    class DeleteTests {

        @Test
        @DisplayName(
                "deletes draft policy successfully"
        )
        void deletePolicy_success() {

            when(
                    policyRepository.findById(
                            1L
                    )
            ).thenReturn(
                    Optional.of(
                            samplePolicy
                    )
            );


            policyService.deletePolicy(
                    1L
            );


            verify(
                    policyRepository
            ).delete(
                    samplePolicy
            );


            verify(
                    policyRepository
            ).flush();
        }


        @Test
        @DisplayName(
                "throws ResourceNotFoundException for missing policy"
        )
        void deletePolicy_notFound() {

            when(
                    policyRepository.findById(
                            99L
                    )
            ).thenReturn(
                    Optional.empty()
            );


            assertThatThrownBy(
                    () ->
                            policyService.deletePolicy(
                                    99L
                            )
            )
                    .isInstanceOf(
                            ResourceNotFoundException.class
                    );


            verify(
                    policyRepository,
                    never()
            ).delete(
                    any(Policy.class)
            );
        }


        @Test
        @DisplayName(
                "cannot delete published policy"
        )
        void deletePolicy_publishedRejected() {

            samplePolicy.setStatus(
                    PolicyStatus.PUBLISHED
            );


            when(
                    policyRepository.findById(
                            1L
                    )
            ).thenReturn(
                    Optional.of(
                            samplePolicy
                    )
            );


            assertThatThrownBy(
                    () ->
                            policyService.deletePolicy(
                                    1L
                            )
            )
                    .isInstanceOf(
                            BadRequestException.class
                    );


            verify(
                    policyRepository,
                    never()
            ).delete(
                    any(Policy.class)
            );
        }
    }
}