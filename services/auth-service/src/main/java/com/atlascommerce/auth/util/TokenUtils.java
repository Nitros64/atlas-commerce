package com.atlascommerce.auth.util;

import java.time.Instant;

public final class TokenUtils {

    private TokenUtils() {} // no instanciable

    public static boolean isInvalid(String token) {
        return token == null || token.isBlank();
    }

    public static boolean isInvalid(String token, Instant date) {
        return isInvalid(token) || date == null;
    }

    // Versión que lanza excepción (útil en muchos casos)
    public static void validate(String token) {
        if (isInvalid(token)) {
            throw new IllegalArgumentException("Token inválido o vacío");
        }
    }
}