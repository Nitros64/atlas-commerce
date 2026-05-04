package com.atlascommerce.auth.service;

import com.atlascommerce.auth.config.SessionProperties;
import com.atlascommerce.auth.entity.RefreshToken;
import com.atlascommerce.auth.entity.UserEntity;
import com.atlascommerce.auth.repository.RefreshTokenRepository;
import com.atlascommerce.auth.util.HashUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionProperties sessionProperties;

    @Transactional
    public RefreshToken createRefreshToken(UserEntity user, String token) {
        enforceMaxSessions(user.getId());
        LocalDateTime now = LocalDateTime.now();

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(HashUtils.sha256(token))
                .user(user)
                .expiryDate(now.plusDays(7))
                .createdAt(now)
                .lastUsedAt(now)
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void revokeRefreshToken(String refreshToken) {
        String tokenHash = HashUtils.sha256(refreshToken);

        refreshTokenRepository.findByTokenHash(HashUtils.sha256(tokenHash))
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }

    @Transactional
    public void revokeAllUserTokens(Long userId) {
        var tokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);

        tokens.forEach(token -> token.setRevoked(true));

        refreshTokenRepository.saveAll(tokens);
    }

    @Transactional
    public void deleteAllUserTokens(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    public boolean isRefreshTokenValid(String refreshToken) {
        String tokenHash = HashUtils.sha256(refreshToken);

        return refreshTokenRepository.findByTokenHash(tokenHash)
                .map(RefreshToken::isValid)
                .orElse(false);
    }
    
    private void enforceMaxSessions(Long userId) {
        List<RefreshToken> activeTokens =
                refreshTokenRepository.findByUserIdAndRevokedFalseAndExpiryDateAfterOrderByCreatedAtAsc(
                        userId,
                        LocalDateTime.now()
                );

        int maxActive = sessionProperties.maxActive();          
        while (activeTokens.size() >= maxActive) {
            RefreshToken oldest = activeTokens.remove(0);
            oldest.setRevoked(true);
            refreshTokenRepository.save(oldest);
        }
    }

    @Transactional
    public void deleteExpiredTokens() {
        // Se puede implementar con una query personalizada
        // TODO
    }
}