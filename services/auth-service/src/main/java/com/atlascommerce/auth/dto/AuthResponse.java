package com.atlascommerce.auth.dto;
import java.util.Set;
import java.util.stream.Collectors;

import com.atlascommerce.auth.entity.UserEntity;

public record AuthResponse(
        String message,
        String email,
        Set<String> roles,
        String accessToken,
        String refreshToken
) {

    public static AuthResponse of(
            String message,
            UserEntity user,
            String accessToken,
            String refreshToken
    ) {
        Set<String> roles = user.getRoles() == null
                ? Set.of()
                : user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toSet());

        return new AuthResponse(
                message,
                user.getEmail(),
                roles,
                accessToken,
                refreshToken
        );
    }
}