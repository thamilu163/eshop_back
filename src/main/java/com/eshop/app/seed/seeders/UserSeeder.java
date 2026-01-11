package com.eshop.app.seed.seeders;

import com.eshop.app.config.properties.SeedProperties;
import com.eshop.app.entity.User;
import com.eshop.app.repository.UserRepository;
import com.eshop.app.seed.core.Seeder;
import com.eshop.app.seed.core.SeederContext;
import com.eshop.app.seed.exception.UserSeedingException;
import com.eshop.app.seed.security.SecurePasswordGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * User seeder - First in execution order.
 * Creates all users from configuration with secure passwords.
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
    
    @Override
    public List<User> seed(SeederContext context) {
        try {
            List<User> users = seedProperties.getUsers().stream()
                .map(this::buildUser)
                .toList();
            
            List<User> savedUsers = userRepository.saveAll(users);
            
            // Populate context for dependent seeders
            context.setUsers(savedUsers.stream()
                .collect(Collectors.toMap(User::getUsername, Function.identity())));
            
            log.info("Seeded {} users successfully", savedUsers.size());
            return savedUsers;
            
        } catch (DataAccessException e) {
            throw new UserSeedingException(
                "Failed to seed users - database constraint violation: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new UserSeedingException(
                "Unexpected error during user seeding: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void cleanup() {
        try {
            userRepository.deleteAllInBatch();
            log.debug("Cleaned up existing users");
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
     * Build User entity from configuration with secure password handling.
     */
    private User buildUser(SeedProperties.UserSeed cfg) {
        String rawPassword = cfg.getPassword();
        if (rawPassword == null || rawPassword.isBlank()) {
            rawPassword = passwordGenerator.generate(cfg.getUsername());
        } else {
            // Password from config, still resolve if it's a reference
            rawPassword = passwordGenerator.generate(cfg.getUsername());
        }
        
        return User.builder()
            .username(cfg.getUsername())
            .email(cfg.getEmail())
            .password(passwordEncoder.encode(rawPassword))
            .firstName(cfg.getFirstName())
            .lastName(cfg.getLastName())
            .phone(cfg.getPhone())
            .address(cfg.getAddress())
            .role(parseRole(cfg.getRole()))
            .sellerType(parseSellerType(cfg.getSellerType()))
            .active(true)
            .emailVerified(true)
            .build();
    }
    
    /**
     * Parse role with fallback to CUSTOMER if invalid.
     */
    private User.UserRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            return User.UserRole.CUSTOMER;
        }
        try {
            return User.UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid role '{}', defaulting to CUSTOMER", role);
            return User.UserRole.CUSTOMER;
        }
    }
    
    /**
     * Parse seller type with null fallback for non-sellers.
     */
    private User.SellerType parseSellerType(String sellerType) {
        if (sellerType == null || sellerType.isBlank()) {
            return null;
        }
        try {
            return User.SellerType.valueOf(sellerType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid sellerType '{}', setting to null", sellerType);
            return null;
        }
    }
}
