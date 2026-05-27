package com.atlascommerce.auth.service;

import com.atlascommerce.auth.config.LoginRateLimitProperties;
import com.atlascommerce.auth.exception.TooManyLoginAttemptsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginRateLimitServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private LoginRateLimitService service() {
        LoginRateLimitProperties properties = 
                new LoginRateLimitProperties(5, 10, 60); // maxAttempts, ? , expirationSeconds

        return new LoginRateLimitService(redisTemplate, properties);
    }

    private String expectedEmailIpKey(String email, String ip) {
        return "rate-limit:login:email-ip:" + hash(normalize(email) + ":" + normalize(ip));
    }

    private String expectedIpKey(String ip) {
        return "rate-limit:login:ip:" + hash(normalize(ip));
    }

    private String normalize(String value) {
        return value == null ? "unknown" : value.trim().toLowerCase();
    }

    @Test
    void checkLoginAllowed_shouldNotThrow_whenAttemptsAreBelowLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("3"); // below limit

        assertDoesNotThrow(() ->
                service().checkLoginAllowed("TEST@EMAIL.COM", "127.0.0.1")
        );
    }

    @Test
    void checkLoginAllowed_shouldThrow_whenLimitIsReached() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("5");

        assertThrows(TooManyLoginAttemptsException.class, () ->
                service().checkLoginAllowed("test@email.com", "127.0.0.1")
        );
    }

    @Test
    void registerFailedAttempt_shouldIncrementBothKeysAndSetExpiration() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        String email = " TEST@EMAIL.COM ";
        String ip = " 127.0.0.1 ";

        service().registerFailedAttempt(email, ip);

        verify(valueOperations).increment(expectedEmailIpKey(email, ip));
        verify(valueOperations).increment(expectedIpKey(ip));

        verify(redisTemplate, times(2))
                .expire(anyString(), eq(Duration.ofSeconds(60)));
    }

    // Helper method (add this)
    private String hash(String input) {
        // Match exactly what your service uses (most likely SHA-256)
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void resetEmailIpAttempts_shouldDeleteEmailIpKey() {
        String email = "test@email.com";
        String ip = "127.0.0.1";

        service().resetEmailIpAttempts(email, ip);

        verify(redisTemplate).delete(startsWith("rate-limit:login:email-ip:"));
    }

    @Test
    void checkLoginAllowed_shouldNormalizeEmailAndIp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("0");

        service().checkLoginAllowed(" TEST@EMAIL.COM ", " 127.0.0.1 ");

        verify(valueOperations).get(startsWith("rate-limit:login:email-ip:"));
        verify(valueOperations).get(startsWith("rate-limit:login:ip:"));
    }
}