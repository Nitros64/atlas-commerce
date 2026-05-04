package com.atlascommerce.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.atlascommerce.auth.entity.RefreshToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    //Optional<RefreshToken> findByToken(String token);
    
    void deleteByUserId(Long userId);
    
    List<RefreshToken> findByUserIdAndRevokedFalse(Long userId);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    List<RefreshToken> findByUserIdAndRevokedFalseAndExpiryDateAfterOrderByCreatedAtAsc(
            Long userId,
            LocalDateTime now
    );

    void deleteByExpiryDateBefore(LocalDateTime now);

    void deleteByRevokedTrueAndExpiryDateBefore(LocalDateTime now);
}