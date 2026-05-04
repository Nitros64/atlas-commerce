package com.atlascommerce.order.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class HashUtils {

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String hexStr = Integer.toHexString(0xff & b);
                if (hexStr.length() == 1) hex.append('0');
                hex.append(hexStr);
            }
            return hex.toString();

        } catch (Exception e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }
}