package com.eshop.app.seed.seeders;

import com.eshop.app.config.properties.SeedProperties;
import com.eshop.app.entity.Store;
import com.eshop.app.entity.User;
import com.eshop.app.repository.StoreRepository;
import com.eshop.app.seed.core.Seeder;
import com.eshop.app.seed.core.SeederContext;
import com.eshop.app.seed.exception.StoreSeedingException;
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
 * Store seeder - Order 5.
 * Creates stores associated with seller users.
 * Depends on UserSeeder.
 */
@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class StoreSeeder implements Seeder<Store, SeederContext> {

    private final StoreRepository storeRepository;
    private final SeedProperties seedProperties;

    @Override
    public List<Store> seed(SeederContext context) {
        try {
            Map<String, User> users = context.getUsers();

            List<Store> storesList = seedProperties.getShops().stream()
                    .map(cfg -> buildStore(cfg, users))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();

            List<Store> savedStores = storeRepository.saveAll(storesList);

            // Populate context
            context.setStores(savedStores.stream()
                    .collect(Collectors.toMap(Store::getStoreName, Function.identity(),
                            (existing, replacement) -> {
                                log.warn("Duplicate store name: {}, keeping first", existing.getStoreName());
                                return existing;
                            })));

            log.info("Seeded {} stores successfully", savedStores.size());
            return savedStores;

        } catch (DataAccessException e) {
            throw new StoreSeedingException(
                    "Failed to seed stores: " + e.getMessage(), e);
        }
    }

    @Override
    public void cleanup() {
        try {
            storeRepository.deleteAllInBatch();
            log.debug("Cleaned up existing stores");
        } catch (Exception e) {
            log.warn("Failed to cleanup stores: {}", e.getMessage());
        }
    }

    @Override
    public int order() {
        return 5;
    }

    @Override
    public String name() {
        return "StoreSeeder";
    }

    /**
     * Build store with null-safe seller lookup.
     * Skips store if seller not found.
     */
    private Optional<Store> buildStore(SeedProperties.ShopSeed cfg, Map<String, User> users) {
        User seller = users.get(cfg.getSellerUsername());

        if (seller == null) {
            log.warn("Skipping store '{}': seller '{}' not found",
                    cfg.getShopName(), cfg.getSellerUsername());
            return Optional.empty();
        }

        return Optional.of(Store.builder()
                .storeName(cfg.getShopName())
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
