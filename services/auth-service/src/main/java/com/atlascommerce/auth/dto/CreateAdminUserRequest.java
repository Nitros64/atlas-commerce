package com.atlascommerce.auth.dto;

import java.util.Set;

import com.atlascommerce.auth.entity.RoleName;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateAdminUserRequest(
        @Email
        @NotBlank
        String email,

        @NotBlank
        @Size(min = 6, max = 100)
        String password,

        @NotEmpty
        Set<RoleName> roles
) {
}