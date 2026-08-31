package com.example.hrmspolicies2.service;

import com.example.hrmspolicies2.dto.PolicyCategoryRequest;
import com.example.hrmspolicies2.dto.response.PolicyCategoryResponse;
import com.example.hrmspolicies2.entity.PolicyCategory;
import com.example.hrmspolicies2.exception.DuplicateResourceException;
import com.example.hrmspolicies2.exception.ResourceNotFoundException;
import com.example.hrmspolicies2.repository.PolicyCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class PolicyCategoryService {

    private final PolicyCategoryRepository categoryRepository;

    public PolicyCategoryService(
            PolicyCategoryRepository categoryRepository
    ) {
        this.categoryRepository =
                categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<PolicyCategoryResponse> getActiveCategories() {
        return categoryRepository
                .findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    public PolicyCategoryResponse createCategory(
            PolicyCategoryRequest request
    ) {
        String name =
                request.getName().trim();

        String code =
                request.getCode()
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (categoryRepository
                .existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException(
                    "Category name already exists"
            );
        }

        if (categoryRepository
                .existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException(
                    "Category code already exists"
            );
        }

        PolicyCategory category =
                PolicyCategory.builder()
                        .name(name)
                        .code(code)
                        .description(
                                request.getDescription()
                        )
                        .active(true)
                        .build();

        return map(
                categoryRepository.save(
                        category
                )
        );
    }

    @Transactional
    public void deactivateCategory(
            Long id
    ) {
        PolicyCategory category =
                categoryRepository
                        .findById(id)
                        .orElseThrow(() ->
                                ResourceNotFoundException
                                        .forEntity(
                                                "PolicyCategory",
                                                id
                                        )
                        );

        category.setActive(false);
        categoryRepository.save(category);
    }

    private PolicyCategoryResponse map(
            PolicyCategory category
    ) {
        return PolicyCategoryResponse
                .builder()
                .id(category.getId())
                .name(category.getName())
                .code(category.getCode())
                .description(
                        category.getDescription()
                )
                .active(category.getActive())
                .build();
    }
}