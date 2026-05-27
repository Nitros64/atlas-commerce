package com.atlascommerce.gateway_service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;



@ExtendWith(MockitoExtension.class)
class JwtAuthConverterTest {

    private final JwtAuthConverter converter = new JwtAuthConverter();

    @Test
    void convert_shouldMapRolesToAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("test@atlas.com")
                .claim("roles", List.of("ROLE_USER", "ROLE_ADMIN"))
                .build();

        AbstractAuthenticationToken authentication =
                converter.convert(jwt).block();

        assertNotNull(authentication);
        assertEquals("test@atlas.com", authentication.getName());

        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));

        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void convert_shouldReturnEmptyAuthorities_whenRolesClaimMissing() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("test@atlas.com")
                .build();

        AbstractAuthenticationToken authentication =
                converter.convert(jwt).block();

        assertNotNull(authentication);
        assertTrue(authentication.getAuthorities().isEmpty());
    }
}