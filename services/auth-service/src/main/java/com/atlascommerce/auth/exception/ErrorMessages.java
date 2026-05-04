package com.atlascommerce.auth.exception;

public final class ErrorMessages {

    private ErrorMessages() {}

    public static final String REDIS_UNAVAILABLE = 
        "Redis is unavailable while blacklisting access token";

    public static final String REDIS_UNEXPECTED_ERROR = 
        "Unexpected error while blacklisting access token";
}