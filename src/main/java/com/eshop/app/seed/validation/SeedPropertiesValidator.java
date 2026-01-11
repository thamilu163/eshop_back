package com.eshop.app.seed.validation;

import com.eshop.app.config.properties.SeedProperties;
import com.eshop.app.seed.exception.InvalidSeedConfigException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates seed configuration for consistency and correctness.
 * Catches configuration errors before attempting to seed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeedPropertiesValidator {
    
    private final Validator validator;
    
    /**
     * Validate entire seed configuration.
     * 
     * @param properties Seed properties to validate
     * @throws InvalidSeedConfigException if validation fails
     */
    public void validate(SeedProperties properties) {
        // JSR-380 validation
        Set<ConstraintViolation<SeedProperties>> violations = validator.validate(properties);
        
        if (!violations.isEmpty()) {
            String errors = violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
            
            throw new InvalidSeedConfigException("Invalid seed configuration: " + errors);
        }
        
        // Business validation
        validateUniqueUsernames(properties.getUsers());
        validateUniqueEmails(properties.getUsers());
        validateShopSellerReferences(properties);
        validateProductReferences(properties);
        
        log.debug("Seed configuration validated successfully");
    }
    
    /**
     * Ensure usernames are unique.
     */
    private void validateUniqueUsernames(List<SeedProperties.UserSeed> users) {
        Set<String> usernames = new HashSet<>();
        List<String> duplicates = users.stream()
            .map(SeedProperties.UserSeed::getUsername)
            .filter(u -> !usernames.add(u))
            .toList();
        
        if (!duplicates.isEmpty()) {
            throw new InvalidSeedConfigException(
                "Duplicate usernames found: " + duplicates);
        }
    }
    
    /**
     * Ensure emails are unique.
     */
    private void validateUniqueEmails(List<SeedProperties.UserSeed> users) {
        Set<String> emails = new HashSet<>();
        List<String> duplicates = users.stream()
            .map(SeedProperties.UserSeed::getEmail)
            .filter(email -> email != null && !email.isBlank())
            .filter(e -> !emails.add(e))
            .toList();
        
        if (!duplicates.isEmpty()) {
            throw new InvalidSeedConfigException(
                "Duplicate emails found: " + duplicates);
        }
    }
    
    /**
     * Validate shop references point to existing users.
     */
    private void validateShopSellerReferences(SeedProperties properties) {
        Set<String> usernames = properties.getUsers().stream()
            .map(SeedProperties.UserSeed::getUsername)
            .collect(Collectors.toSet());
        
        List<String> invalidShops = properties.getShops().stream()
            .filter(shop -> shop.getSellerUsername() != null)
            .filter(shop -> !usernames.contains(shop.getSellerUsername()))
            .map(shop -> shop.getShopName() + " -> " + shop.getSellerUsername())
            .toList();
        
        if (!invalidShops.isEmpty()) {
            log.warn("Shops reference non-existent sellers (will be skipped): {}", invalidShops);
        }
    }
    
    /**
     * Validate product references point to existing catalog items.
     */
    private void validateProductReferences(SeedProperties properties) {
        Set<String> categoryNames = properties.getCategories().stream()
            .map(SeedProperties.CategorySeed::getName)
            .collect(Collectors.toSet());
        
        Set<String> shopNames = properties.getShops().stream()
            .map(SeedProperties.ShopSeed::getShopName)
            .collect(Collectors.toSet());
        
        List<String> productsWithMissingCategory = properties.getProducts().stream()
            .filter(p -> p.getCategoryName() != null)
            .filter(p -> !categoryNames.contains(p.getCategoryName()))
            .map(SeedProperties.ProductSeed::getName)
            .toList();
        
        List<String> productsWithMissingShop = properties.getProducts().stream()
            .filter(p -> p.getShopName() != null)
            .filter(p -> !shopNames.contains(p.getShopName()))
            .map(SeedProperties.ProductSeed::getName)
            .toList();
        
        if (!productsWithMissingCategory.isEmpty()) {
            log.warn("Products reference non-existent categories (will be skipped): {}", 
                productsWithMissingCategory);
        }
        
        if (!productsWithMissingShop.isEmpty()) {
            log.warn("Products reference non-existent shops (will be skipped): {}", 
                productsWithMissingShop);
        }
    }
}
