package com.example.hrmspolicies2.repository;

import com.example.hrmspolicies2.entity.PasswordResetToken;
import com.example.hrmspolicies2.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    void deleteByUser(User user);
}