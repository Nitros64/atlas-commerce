package com.atlascommerce.auth.config;

import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.atlascommerce.auth.entity.Role;
import com.atlascommerce.auth.entity.RoleName;
import com.atlascommerce.auth.entity.UserEntity;
import com.atlascommerce.auth.repository.RoleRepository;
import com.atlascommerce.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        String adminEmail = "admin@atlas.com";

        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN not found"));

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER not found"));

        UserEntity admin = UserEntity.builder()
                .email(adminEmail)
                .username(adminEmail)
                .password(passwordEncoder.encode("Admin123*"))
                .roles(Set.of(adminRole, userRole))
                .enabled(true)
                .build();

        userRepository.save(admin);
    }
}