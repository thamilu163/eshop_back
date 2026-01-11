package com.eshop.app.seed.seeders;

import com.eshop.app.config.properties.SeedProperties;
import com.eshop.app.entity.Shop;
import com.eshop.app.entity.User;
import com.eshop.app.repository.ShopRepository;
import com.eshop.app.seed.core.Seeder;
import com.eshop.app.seed.core.SeederContext;
import com.eshop.app.seed.exception.ShopSeedingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Shop seeder - Order 5.
 * Creates shops associated with seller users.
 * Depends on UserSeeder.
 */
@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class ShopSeeder implements Seeder<Shop, SeederContext> {
    
    private final ShopRepository shopRepository;
    private final SeedProperties seedProperties;
    
    @Override
    public List<Shop> seed(SeederContext context) {
        try {
            Map<String, User> users = context.getUsers();
            
            List<Shop> shops = seedProperties.getShops().stream()
                .map(cfg -> buildShop(cfg, users))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
            
            List<Shop> savedShops = shopRepository.saveAll(shops);
            
            // Populate context
            context.setShops(savedShops.stream()
                .collect(Collectors.toMap(Shop::getShopName, Function.identity(),
                    (existing, replacement) -> {
                        log.warn("Duplicate shop name: {}, keeping first", existing.getShopName());
                        return existing;
                    })));
            
            log.info("Seeded {} shops successfully", savedShops.size());
            return savedShops;
            
        } catch (DataAccessException e) {
            throw new ShopSeedingException(
                "Failed to seed shops: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void cleanup() {
        try {
            shopRepository.deleteAllInBatch();
            log.debug("Cleaned up existing shops");
        } catch (Exception e) {
            log.warn("Failed to cleanup shops: {}", e.getMessage());
        }
    }
    
    @Override
    public int order() {
        return 5;
    }
    
    @Override
    public String name() {
        return "ShopSeeder";
    }
    
    /**
     * Build shop with null-safe seller lookup.
     * Skips shop if seller not found.
     */
    private Optional<Shop> buildShop(SeedProperties.ShopSeed cfg, Map<String, User> users) {
        User seller = users.get(cfg.getSellerUsername());
        
        if (seller == null) {
            log.warn("Skipping shop '{}': seller '{}' not found",
                cfg.getShopName(), cfg.getSellerUsername());
            return Optional.empty();
        }
        
        return Optional.of(Shop.builder()
            .shopName(cfg.getShopName())
            .description(cfg.getDescription())
            .address(cfg.getAddress())
            .phone(cfg.getPhone())
            .email(cfg.getEmail())
            .logoUrl(cfg.getLogoUrl())
            .seller(seller)
            .sellerType(parseSellerType(cfg.getSellerType()))
            .active(true)
            .build());
    }
    
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
