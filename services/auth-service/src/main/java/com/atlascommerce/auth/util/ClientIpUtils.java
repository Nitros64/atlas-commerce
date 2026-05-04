package com.atlascommerce.auth.util;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class ClientIpUtils {
    public String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "0.0.0.0";
        }

        // Orden de prioridad recomendado
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "CF-Connecting-IP",        // Cloudflare
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_CLIENT_IP"
        };

        for (String header : headers) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                // Tomamos la primera IP (la del cliente real)
                return value.split(",")[0].trim();
            }
        }

        // Fallback
        return request.getRemoteAddr();
    }
}
