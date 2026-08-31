package com.example.hrmspolicies2.service;

import com.example.hrmspolicies2.dto.PolicyRequest;
import com.example.hrmspolicies2.dto.response.PageResponse;
import com.example.hrmspolicies2.entity.Policy;
import com.example.hrmspolicies2.exception.BadRequestException;
import com.example.hrmspolicies2.exception.DuplicateResourceException;
import com.example.hrmspolicies2.exception.ResourceNotFoundException;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyService Unit Tests")
class PolicyServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PolicyService policyService;

    private Policy samplePolicy;
    private PolicyRequest sampleRequest;

    @BeforeEach
    void setUp() {

        SecurityContextHolder.clearContext();

        samplePolicy = Policy.builder()
                .id(1L)
                .name("Work From Home Policy")
                .code("WFH-001")
                .category("Leave")
                .content(
                        "Employees may work remotely up to 3 days per week.")
                .applicability("ALL")
                .mandatory(true)
                .status("DRAFT")
                .build();

        sampleRequest = new PolicyRequest();

        sampleRequest.setName("Work From Home Policy");
        sampleRequest.setCode("WFH-001");
        sampleRequest.setCategory("Leave");
        sampleRequest.setContent(
                "Employees may work remotely up to 3 days per week.");
        sampleRequest.setApplicability("ALL");
        sampleRequest.setMandatory(true);
        sampleRequest.setStatus("DRAFT");
    }

    @AfterEach
    void tearDown() {

        SecurityContextHolder.clearContext();
    }

    // ==========================================================
    // GET ALL
    // ==========================================================

    @Nested
    @DisplayName("getAllPolicies()")
    class GetAllTests {

        @Test
        @DisplayName("Positive - returns all policies")
        void getAllPolicies_returnsList() {

            when(policyRepository.findAll())
                    .thenReturn(List.of(samplePolicy));

            List<Policy> result =
                    policyService.getAllPolicies();

            assertThat(result)
                    .hasSize(1)
                    .containsExactly(samplePolicy);
        }

        @Test
        @DisplayName("Positive - returns empty list when no policies exist")
        void getAllPolicies_empty() {

            when(policyRepository.findAll())
                    .thenReturn(List.of());

            List<Policy> result =
                    policyService.getAllPolicies();

            assertThat(result).isEmpty();
        }
    }

    // ==========================================================
    // GET BY ID
    // ==========================================================

    @Nested
    @DisplayName("getPolicyById()")
    class GetByIdTests {

        @Test
        @DisplayName("Positive - returns policy when id exists")
        void getPolicyById_found() {

            when(policyRepository.findById(1L))
                    .thenReturn(Optional.of(samplePolicy));

            Policy result =
                    policyService.getPolicyById(1L);

            assertThat(result)
                    .isEqualTo(samplePolicy);
        }

        @Test
        @DisplayName("Negative - throws exception when policy does not exist")
        void getPolicyById_notFound_throws() {

            when(policyRepository.findById(99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    policyService.getPolicyById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Policy not found with id: 99");
        }
    }

    // ==========================================================
    // SEARCH
    // ==========================================================

    @Nested
    @DisplayName("searchPolicies()")
    class SearchTests {

        @Test
        @DisplayName("Positive - searches with valid sort field")
        void searchPolicies_validSortField_success() {

            Page<Policy> page =
                    new PageImpl<>(
                            List.of(samplePolicy),
                            PageRequest.of(0, 10),
                            1
                    );

            when(policyRepository.findAll(
                    any(Specification.class),
                    any(PageRequest.class)))
                    .thenReturn(page);

            PageResponse<Policy> result =
                    policyService.searchPolicies(
                            "home",
                            "Leave",
                            "DRAFT",
                            "ALL",
                            true,
                            0,
                            10,
                            "name",
                            "asc"
                    );

            assertThat(result.getContent())
                    .containsExactly(samplePolicy);

            assertThat(result.getTotalElements())
                    .isEqualTo(1);

            assertThat(result.getPageNumber())
                    .isEqualTo(0);
        }

        @Test
        @DisplayName("Positive - blank sort field defaults to id")
        void searchPolicies_blankSortBy_defaultsToId() {

            Page<Policy> page =
                    new PageImpl<>(List.of(samplePolicy));

            when(policyRepository.findAll(
                    any(Specification.class),
                    any(PageRequest.class)))
                    .thenReturn(page);

            assertThatCode(() ->
                    policyService.searchPolicies(
                            null,
                            null,
                            null,
                            null,
                            null,
                            0,
                            10,
                            "  ",
                            "asc"
                    )
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Negative - invalid sort field throws BadRequestException")
        void searchPolicies_invalidSortField_throws() {

            assertThatThrownBy(() ->
                    policyService.searchPolicies(
                            null,
                            null,
                            null,
                            null,
                            null,
                            0,
                            10,
                            "notAField",
                            "asc"
                    )
            )
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining(
                            "Invalid sortBy field 'notAField'");

            verify(
                    policyRepository,
                    never()
            ).findAll(
                    any(Specification.class),
                    any(PageRequest.class)
            );
        }

        @Test
        @DisplayName("Positive - invalid size falls back to 10")
        void searchPolicies_nonPositiveSize_defaultsToTen() {

            ArgumentCaptor<PageRequest> captor =
                    ArgumentCaptor.forClass(PageRequest.class);

            Page<Policy> page =
                    new PageImpl<>(List.of(samplePolicy));

            when(policyRepository.findAll(
                    any(Specification.class),
                    captor.capture()))
                    .thenReturn(page);

            policyService.searchPolicies(
                    null,
                    null,
                    null,
                    null,
                    null,
                    0,
                    0,
                    "id",
                    "asc"
            );

            assertThat(captor.getValue().getPageSize())
                    .isEqualTo(10);
        }

        @Test
        @DisplayName("Positive - negative page number is clamped to zero")
        void searchPolicies_negativePage_defaultsToZero() {

            ArgumentCaptor<PageRequest> captor =
                    ArgumentCaptor.forClass(PageRequest.class);

            Page<Policy> page =
                    new PageImpl<>(List.of(samplePolicy));

            when(policyRepository.findAll(
                    any(Specification.class),
                    captor.capture()))
                    .thenReturn(page);

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

            assertThat(captor.getValue().getPageNumber())
                    .isEqualTo(0);
        }
    }

    // ==========================================================
    // CREATE
    // ==========================================================

    @Nested
    @DisplayName("createPolicy()")
    class CreateTests {

        @Test
        @DisplayName("Positive - creates policy successfully")
        void createPolicy_success() {

            when(policyRepository.existsByCode("WFH-001"))
                    .thenReturn(false);

            when(policyRepository.save(any(Policy.class)))
                    .thenReturn(samplePolicy);

            Policy result =
                    policyService.createPolicy(sampleRequest);

            assertThat(result)
                    .isEqualTo(samplePolicy);

            verify(policyRepository, times(1))
                    .save(any(Policy.class));
        }

        @Test
        @DisplayName("Positive - null status defaults to DRAFT")
        void createPolicy_nullStatus_defaultsToDraft() {

            sampleRequest.setStatus(null);

            when(policyRepository.existsByCode("WFH-001"))
                    .thenReturn(false);

            ArgumentCaptor<Policy> captor =
                    ArgumentCaptor.forClass(Policy.class);

            when(policyRepository.save(captor.capture()))
                    .thenReturn(samplePolicy);

            policyService.createPolicy(sampleRequest);

            assertThat(captor.getValue().getStatus())
                    .isEqualTo("DRAFT");
        }

        @Test
        @DisplayName("Negative - duplicate policy code throws exception")
        void createPolicy_duplicateCode_throws() {

            when(policyRepository.existsByCode("WFH-001"))
                    .thenReturn(true);

            assertThatThrownBy(() ->
                    policyService.createPolicy(sampleRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessage(
                            "Policy code already exists: WFH-001");

            verify(
                    policyRepository,
                    never()
            ).save(any(Policy.class));
        }
    }

    // ==========================================================
    // UPDATE
    // ==========================================================

    @Nested
    @DisplayName("updatePolicy()")
    class UpdateTests {

        @Test
        @DisplayName("Positive - updates policy successfully")
        void updatePolicy_success() {

            when(policyRepository.findById(1L))
                    .thenReturn(Optional.of(samplePolicy));

            when(policyRepository.save(any(Policy.class)))
                    .thenAnswer(invocation ->
                            invocation.getArgument(0));

            sampleRequest.setName(
                    "Work From Home Policy Revised");

            Policy result =
                    policyService.updatePolicy(
                            1L,
                            sampleRequest
                    );

            assertThat(result.getName())
                    .isEqualTo(
                            "Work From Home Policy Revised");

            verify(
                    policyRepository,
                    never()
            ).existsByCode(anyString());
        }

        @Test
        @DisplayName("Negative - policy id does not exist")
        void updatePolicy_notFound_throws() {

            when(policyRepository.findById(99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    policyService.updatePolicy(
                            99L,
                            sampleRequest
                    ))
                    .isInstanceOf(
                            ResourceNotFoundException.class)
                    .hasMessage(
                            "Policy not found with id: 99");

            verify(
                    policyRepository,
                    never()
            ).save(any(Policy.class));
        }

        @Test
        @DisplayName("Negative - updated code already exists")
        void updatePolicy_duplicateCode_throws() {

            when(policyRepository.findById(1L))
                    .thenReturn(Optional.of(samplePolicy));

            sampleRequest.setCode("WFH-002");

            when(policyRepository.existsByCode("WFH-002"))
                    .thenReturn(true);

            assertThatThrownBy(() ->
                    policyService.updatePolicy(
                            1L,
                            sampleRequest
                    ))
                    .isInstanceOf(
                            DuplicateResourceException.class)
                    .hasMessage(
                            "Policy code already exists: WFH-002");

            verify(
                    policyRepository,
                    never()
            ).save(any(Policy.class));
        }
    }

    // ==========================================================
    // PATCH
    // ==========================================================

    @Nested
    @DisplayName("patchPolicy()")
    class PatchTests {

        @Test
        @DisplayName("Positive - updates only supplied fields")
        void patchPolicy_partialUpdate_success() {

            when(policyRepository.findById(1L))
                    .thenReturn(Optional.of(samplePolicy));

            when(policyRepository.save(any(Policy.class)))
                    .thenAnswer(invocation ->
                            invocation.getArgument(0));

            PolicyRequest partial =
                    new PolicyRequest();

            partial.setStatus("ACTIVE");

            Policy result =
                    policyService.patchPolicy(
                            1L,
                            partial
                    );

            assertThat(result.getStatus())
                    .isEqualTo("ACTIVE");

            assertThat(result.getName())
                    .isEqualTo(
                            "Work From Home Policy");

            assertThat(result.getCode())
                    .isEqualTo("WFH-001");
        }

        @Test
        @DisplayName("Negative - policy id does not exist")
        void patchPolicy_notFound_throws() {

            when(policyRepository.findById(99L))
                    .thenReturn(Optional.empty());

            PolicyRequest partial =
                    new PolicyRequest();

            partial.setStatus("ACTIVE");

            assertThatThrownBy(() ->
                    policyService.patchPolicy(
                            99L,
                            partial
                    ))
                    .isInstanceOf(
                            ResourceNotFoundException.class)
                    .hasMessage(
                            "Policy not found with id: 99");
        }

        @Test
        @DisplayName("Negative - patched code already exists")
        void patchPolicy_duplicateCode_throws() {

            when(policyRepository.findById(1L))
                    .thenReturn(Optional.of(samplePolicy));

            PolicyRequest partial =
                    new PolicyRequest();

            partial.setCode("WFH-999");

            when(policyRepository.existsByCode("WFH-999"))
                    .thenReturn(true);

            assertThatThrownBy(() ->
                    policyService.patchPolicy(
                            1L,
                            partial
                    ))
                    .isInstanceOf(
                            DuplicateResourceException.class)
                    .hasMessage(
                            "Policy code already exists: WFH-999");

            verify(
                    policyRepository,
                    never()
            ).save(any(Policy.class));
        }

        @Test
        @DisplayName("Positive - same code does not trigger duplicate check")
        void patchPolicy_sameCode_noUniquenessCheck() {

            when(policyRepository.findById(1L))
                    .thenReturn(Optional.of(samplePolicy));

            when(policyRepository.save(any(Policy.class)))
                    .thenAnswer(invocation ->
                            invocation.getArgument(0));

            PolicyRequest partial =
                    new PolicyRequest();

            partial.setCode("wfh-001");

            policyService.patchPolicy(
                    1L,
                    partial
            );

            verify(
                    policyRepository,
                    never()
            ).existsByCode(anyString());
        }
    }

    // ==========================================================
    // DELETE
    // ==========================================================

    @Nested
    @DisplayName("deletePolicy()")
    class DeleteTests {

        @Test
        @DisplayName("Positive - deletes existing policy")
        void deletePolicy_success() {

            when(policyRepository.findById(1L))
                    .thenReturn(Optional.of(samplePolicy));

            policyService.deletePolicy(1L);

            verify(
                    policyRepository,
                    times(1)
            ).delete(samplePolicy);
        }

        @Test
        @DisplayName("Negative - policy id does not exist")
        void deletePolicy_notFound_throws() {

            when(policyRepository.findById(99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    policyService.deletePolicy(99L))
                    .isInstanceOf(
                            ResourceNotFoundException.class)
                    .hasMessage(
                            "Policy not found with id: 99");

            /*
             * Important:
             * Specify Policy.class because PolicyRepository
             * also has JpaSpecificationExecutor.delete(...)
             * and Mockito otherwise sees any() as ambiguous.
             */
            verify(
                    policyRepository,
                    never()
            ).delete(any(Policy.class));
        }
    }
}