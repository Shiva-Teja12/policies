package com.example.hrmspolicies2.config;

import com.example.hrmspolicies2.entity.User;
import com.example.hrmspolicies2.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds a default ADMIN account on first startup so the API can be
 * exercised immediately (Postman / Swagger) without a separate
 * bootstrap step. In a real production system this would be replaced
 * by a proper migration + manually provisioned credentials.
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initializeData(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {

        return args -> {

            String adminEmail =
                    "admin@gmail.com";

            if (!userRepository
                    .existsByEmail(adminEmail)) {

                User admin =
                        User.builder()
                                .name("HR Admin")
                                .email(adminEmail)
                                .password(
                                        passwordEncoder.encode(
                                                "admin123"
                                        )
                                )
                                .role("ADMIN")
                                .build();

                userRepository.save(admin);

                // Demo/seed credentials only - never log real user
                // passwords in a production system.
                log.info("=================================");
                log.info("Default admin account created (demo/dev only)");
                log.info("Email: {}", adminEmail);
                log.info("Password: admin123");
                log.info("=================================");
            }
        };
    }
}
