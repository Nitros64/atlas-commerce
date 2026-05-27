package com.atlascommerce.auth.service;

import com.atlascommerce.auth.config.SessionProperties;
import com.atlascommerce.auth.entity.RefreshToken;
import com.atlascommerce.auth.entity.UserEntity;
import com.atlascommerce.auth.repository.RefreshTokenRepository;
import com.atlascommerce.auth.util.HashUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private SessionProperties sessionProperties;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private UserEntity user() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmail("test@atlas.com");
        return user;
    }

    private RefreshToken refreshToken(boolean revoked) {
        return RefreshToken.builder()
                .tokenHash(HashUtils.sha256("refresh-token"))
                .user(user())
                .expiryDate(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .lastUsedAt(LocalDateTime.now())
                .revoked(revoked)
                .build();
    }

    @Test
    void createRefreshToken_shouldSaveNewToken() {
        UserEntity user = user();

        when(sessionProperties.maxActive()).thenReturn(3);
        when(refreshTokenRepository
                .findByUserIdAndRevokedFalseAndExpiryDateAfterOrderByCreatedAtAsc(
                        eq(1L), any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken result = refreshTokenService.createRefreshToken(user, "refresh-token");

        assertNotNull(result);
        assertEquals(HashUtils.sha256("refresh-token"), result.getTokenHash());
        assertEquals(user, result.getUser());
        assertFalse(result.isRevoked());
        assertNotNull(result.getExpiryDate());
        assertTrue(result.getExpiryDate().isAfter(LocalDateTime.now()));

        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(refreshTokenRepository, never()).saveAll(any());
    }

    @Test
    void createRefreshToken_shouldRevokeOldestToken_whenMaxActiveSessionsReached() {
        UserEntity user = user();
        RefreshToken oldest = refreshToken(false);
        RefreshToken newer = refreshToken(false);

        when(sessionProperties.maxActive()).thenReturn(2);
        when(refreshTokenRepository
                .findByUserIdAndRevokedFalseAndExpiryDateAfterOrderByCreatedAtAsc(
                        eq(1L), any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>(List.of(oldest, newer)));

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        refreshTokenService.createRefreshToken(user, "new-refresh-token");

        assertTrue(oldest.isRevoked(), "Oldest token should be revoked");
        assertFalse(newer.isRevoked(), "Newer token should remain active");

        verify(refreshTokenRepository).save(oldest);        // revoke oldest
        verify(refreshTokenRepository, atLeastOnce()).save(any(RefreshToken.class)); // new token
    }

    @Test
    void isRefreshTokenValid_shouldReturnTrue_forValidToken() {
        RefreshToken token = refreshToken(false);

        when(refreshTokenRepository.findByTokenHash(HashUtils.sha256("refresh-token")))
                .thenReturn(Optional.of(token));

        assertTrue(refreshTokenService.isRefreshTokenValid("refresh-token"));
    }

    @Test
    void isRefreshTokenValid_shouldReturnFalse_whenTokenNotFound() {
        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.empty());

        assertFalse(refreshTokenService.isRefreshTokenValid("refresh-token"));
    }

    @Test
    void isRefreshTokenValid_shouldReturnFalse_whenTokenIsRevoked() {
        RefreshToken revokedToken = refreshToken(true);

        when(refreshTokenRepository.findByTokenHash(HashUtils.sha256("refresh-token")))
                .thenReturn(Optional.of(revokedToken));

        assertFalse(refreshTokenService.isRefreshTokenValid("refresh-token"));
    }

    @Test
    void revokeRefreshToken_shouldMarkTokenAsRevoked() {
        RefreshToken token = refreshToken(false);

        when(refreshTokenRepository.findByTokenHash(HashUtils.sha256("refresh-token")))
                .thenReturn(Optional.of(token));

        refreshTokenService.revokeRefreshToken("refresh-token");

        assertTrue(token.isRevoked());
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void revokeRefreshToken_shouldDoNothing_whenTokenNotFound() {
        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> 
                refreshTokenService.revokeRefreshToken("non-existent-token"));

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void revokeAllUserTokens_shouldRevokeAllActiveTokens() {
        RefreshToken token1 = refreshToken(false);
        RefreshToken token2 = refreshToken(false);
        List<RefreshToken> tokens = List.of(token1, token2);

        when(refreshTokenRepository.findByUserIdAndRevokedFalse(1L))
                .thenReturn(tokens);

        refreshTokenService.revokeAllUserTokens(1L);

        assertTrue(token1.isRevoked());
        assertTrue(token2.isRevoked());

        verify(refreshTokenRepository).saveAll(tokens);
    }
}