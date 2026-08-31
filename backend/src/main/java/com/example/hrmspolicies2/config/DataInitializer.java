package com.example.hrmspolicies2.config;

import com.example.hrmspolicies2.entity.PolicyCategory;
import com.example.hrmspolicies2.entity.User;
import com.example.hrmspolicies2.enums.AccountStatus;
import com.example.hrmspolicies2.enums.Role;
import com.example.hrmspolicies2.repository.PolicyCategoryRepository;
import com.example.hrmspolicies2.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Configuration
@Profile("dev")
public class DataInitializer {

    @Bean
    CommandLineRunner initializeData(
            UserRepository userRepository,
            PolicyCategoryRepository categoryRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {

            createOrUpdateUser(
                    userRepository,
                    passwordEncoder,
                    "HR Admin",
                    "hradmin@enfec.com",
                    Role.HR_ADMIN,
                    "HR",
                    "A1",
                    null
            );

            createOrUpdateUser(
                    userRepository,
                    passwordEncoder,
                    "Legal Reviewer",
                    "legal@enfec.com",
                    Role.LEGAL_REVIEWER,
                    "Legal",
                    "A1",
                    "hradmin@enfec.com"
            );

            createOrUpdateUser(
                    userRepository,
                    passwordEncoder,
                    "HR Head",
                    "hrhead@enfec.com",
                    Role.HR_HEAD,
                    "HR",
                    "A1",
                    "md@enfec.com"
            );

            createOrUpdateUser(
                    userRepository,
                    passwordEncoder,
                    "Managing Director",
                    "md@enfec.com",
                    Role.MANAGING_DIRECTOR,
                    "Management",
                    "A0",
                    null
            );

            createOrUpdateUser(
                    userRepository,
                    passwordEncoder,
                    "Manager",
                    "manager@enfec.com",
                    Role.MANAGER,
                    "Operations",
                    "B1",
                    "hrhead@enfec.com"
            );

            createOrUpdateUser(
                    userRepository,
                    passwordEncoder,
                    "Employee",
                    "employee@enfec.com",
                    Role.EMPLOYEE,
                    "IT",
                    "C1",
                    "manager@enfec.com"
            );


            createCategory(
                    categoryRepository,
                    "Human Resources",
                    "HR"
            );

            createCategory(
                    categoryRepository,
                    "Information Technology",
                    "IT"
            );

            createCategory(
                    categoryRepository,
                    "Finance",
                    "FIN"
            );

            createCategory(
                    categoryRepository,
                    "Compliance",
                    "COMPLIANCE"
            );

            createCategory(
                    categoryRepository,
                    "General",
                    "GENERAL"
            );
        };
    }


    private void createOrUpdateUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String name,
            String email,
            Role role,
            String department,
            String grade,
            String managerEmail
    ) {

        User user = userRepository
                .findByEmailIgnoreCase(email)
                .orElse(null);

        if (user == null) {

            user = User.builder()
                    .name(name)
                    .email(email)
                    .password(
                            passwordEncoder.encode(
                                    "Password@123"
                            )
                    )
                    .role(role)
                    .accountStatus(AccountStatus.ACTIVE)
                    .dateOfJoining(LocalDate.now())
                    .department(department)
                    .grade(grade)
                    .managerEmail(managerEmail)
                    .build();

        } else {

            user.setName(name);
            user.setRole(role);
            user.setAccountStatus(AccountStatus.ACTIVE);

            if (user.getDateOfJoining() == null) {
                user.setDateOfJoining(LocalDate.now());
            }

            user.setDepartment(department);
            user.setGrade(grade);
            user.setManagerEmail(managerEmail);
        }

        userRepository.save(user);
    }


    private void createCategory(
            PolicyCategoryRepository categoryRepository,
            String name,
            String code
    ) {

        if (categoryRepository.existsByCodeIgnoreCase(code)) {
            return;
        }

        PolicyCategory category =
                PolicyCategory.builder()
                        .name(name)
                        .code(code)
                        .active(true)
                        .build();

        categoryRepository.save(category);
    }
}