package com.atlascommerce.auth.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class LogoutRequest {
    private final String refreshToken; 
}