package com.atlascommerce.auth.exception;

public final class RedisErrorMessages {
    public static final String REDIS_CONNECTION_ERROR = "Error occurred while connecting to Redis";
    public static final String REDIS_OPERATION_ERROR = "Error occurred while performing operation on Redis";
    public static final String REDIS_UNAVAILABLE_ERROR = "Redis unavailable while checking user-wide token revocation. Falling back to JWT-only validation.";
}
