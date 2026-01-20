package com.eshop.app.seed.seeders;

import com.eshop.app.config.properties.SeedProperties;
import com.eshop.app.dto.auth.RegisterRequest;
import com.eshop.app.entity.User;
import com.eshop.app.repository.UserRepository;
import com.eshop.app.seed.core.Seeder;
import com.eshop.app.seed.core.SeederContext;
import com.eshop.app.seed.exception.UserSeedingException;
import com.eshop.app.seed.security.SecurePasswordGenerator;
import com.eshop.app.service.auth.KeycloakAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.eshop.app.enums.UserRole;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import reactor.core.publisher.Mono;

/**
 * User seeder - First in execution order.
 * Creates all users from configuration with secure passwords.
 * Ensures users exist in both Keycloak (Auth provider) and Local DB (Business
 * logic).
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class UserSeeder implements Seeder<User, SeederContext> {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurePasswordGenerator passwordGenerator;
    private final SeedProperties seedProperties;
    private final KeycloakAdminService keycloakAdminService;

    @Override
    public List<User> seed(SeederContext context) {
        // If disabled, just load existing users to context so other seeders can
        // function
        if (!seedProperties.isUsersEnabled()) {
            log.info("User seeding disabled. Loading existing users into context...");
            List<User> existingUsers = userRepository.findAll();
            populateContext(context, existingUsers);
            return existingUsers;
        }

        try {
            List<User> users = seedProperties.getUsers().stream()
                    .map(this::processAndBuildUser)
                    .toList();

            List<User> savedUsers = userRepository.saveAll(users);
            populateContext(context, savedUsers);

            log.info("Seeded {} users successfully to Local DB and Keycloak", savedUsers.size());
            return savedUsers;

        } catch (DataAccessException e) {
            throw new UserSeedingException(
                    "Failed to seed users - database constraint violation: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new UserSeedingException(
                    "Unexpected error during user seeding: " + e.getMessage(), e);
        }
    }

    private void populateContext(SeederContext context, List<User> users) {
        context.setUsers(users.stream()
                .collect(Collectors.toMap(User::getUsername, Function.identity())));
    }

    @Override
    public void cleanup() {
        if (!seedProperties.isUsersEnabled()) {
            log.info("User seeding disabled, skipping cleanup (deletion) of users");
            return;
        }

        try {
            // Note: We only clean up local DB. Cleaning up Keycloak is risky/complex for
            // dev
            userRepository.deleteAllInBatch();
            log.debug("Cleaned up existing users from Local DB");
        } catch (Exception e) {
            log.warn("Failed to cleanup users: {}", e.getMessage());
        }
    }

    @Override
    public int order() {
        return 1;
    }

    @Override
    public String name() {
        return "UserSeeder";
    }

    /**
     * Process user: Create in Keycloak if needed, then build local entity.
     */
    private User processAndBuildUser(SeedProperties.UserSeed cfg) {
        // 1. Resolve Password
        String rawPassword = cfg.getPassword();
        if (rawPassword == null || rawPassword.isBlank()) {
            rawPassword = passwordGenerator.generate(cfg.getUsername());
        }

        // 2. Create in Keycloak
        createKeycloakUser(cfg, rawPassword);

        // 3. Build Local Entity
        return User.builder()
                .username(cfg.getUsername())
                .email(cfg.getEmail())
                .password(passwordEncoder.encode(rawPassword)) // We still hash it locally even if unused for login
                .firstName(cfg.getFirstName())
                .lastName(cfg.getLastName())
                .phone(cfg.getPhone())
                .address(cfg.getAddress())
                .role(parseRole(cfg.getRole()))
                .active(true)
                .emailVerified(true) // Keycloak handles this, but we mark local as verified
                .build();
    }

    private void createKeycloakUser(SeedProperties.UserSeed cfg, String password) {
        try {
            RegisterRequest request = RegisterRequest.builder()
                    .username(cfg.getUsername())
                    .email(cfg.getEmail())
                    .password(password)
                    .firstName(cfg.getFirstName())
                    .lastName(cfg.getLastName())
                    .enabled(true)
                    .build();

            // We use block() here because Seeding is a startup sync process
            keycloakAdminService.createUser(request)
                    .doOnError(e -> log.warn("Keycloak user creation warning for {}: {}", cfg.getUsername(),
                            e.getMessage()))
                    .onErrorResume(e -> Mono.empty()) // Continue even if user exists
                    .block();

        } catch (Exception e) {
            // Log but don't fail the whole seeding - user might already exist
            log.warn("Failed to create Keycloak user '{}': {}", cfg.getUsername(), e.getMessage());
        }
    }

    /**
     * Parse role with fallback to CUSTOMER if invalid.
     */
    private UserRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            return UserRole.CUSTOMER;
        }
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid role '{}', defaulting to CUSTOMER", role);
            return UserRole.CUSTOMER;
        }
    }

}
