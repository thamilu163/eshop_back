package com.eshop.app.seed.seeders;

import com.eshop.app.config.properties.SeedProperties;
import com.eshop.app.entity.Brand;
import com.eshop.app.repository.BrandRepository;
import com.eshop.app.seed.core.Seeder;
import com.eshop.app.seed.core.SeederContext;
import com.eshop.app.seed.exception.CatalogSeedingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Brand seeder - Order 3.
 * Creates product brands.
 */
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class BrandSeeder implements Seeder<Brand, SeederContext> {
    
    private final BrandRepository brandRepository;
    private final SeedProperties seedProperties;
    
    @Override
    public List<Brand> seed(SeederContext context) {
        try {
            List<Brand> brands = seedProperties.getBrands().stream()
                .map(this::buildBrand)
                .toList();
            
            List<Brand> savedBrands = brandRepository.saveAll(brands);
            
            // Populate context
            context.setBrands(savedBrands.stream()
                .collect(Collectors.toMap(Brand::getName, Function.identity(),
                    (existing, replacement) -> {
                        log.warn("Duplicate brand name: {}, keeping first", existing.getName());
                        return existing;
                    })));
            
            log.info("Seeded {} brands successfully", savedBrands.size());
            return savedBrands;
            
        } catch (DataAccessException e) {
            throw new CatalogSeedingException(
                "Failed to seed brands: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void cleanup() {
        try {
            brandRepository.deleteAllInBatch();
            log.debug("Cleaned up existing brands");
        } catch (Exception e) {
            log.warn("Failed to cleanup brands: {}", e.getMessage());
        }
    }
    
    @Override
    public int order() {
        return 3;
    }
    
    @Override
    public String name() {
        return "BrandSeeder";
    }
    
    private Brand buildBrand(SeedProperties.BrandSeed cfg) {
        return Brand.builder()
            .name(cfg.getName())
            .description(cfg.getDescription())
            .logoUrl(cfg.getLogoUrl())
            .active(true)
            .build();
    }
}
