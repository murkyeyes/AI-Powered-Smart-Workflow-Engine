package com.workflow.engine.repository;

import com.workflow.engine.model.RefreshToken;
import com.workflow.engine.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);
    int deleteByUser(User user);
    int deleteByToken(String token);
}
