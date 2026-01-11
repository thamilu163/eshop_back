package com.eshop.app.seed.seeders;

import com.eshop.app.config.properties.SeedProperties;
import com.eshop.app.entity.Category;
import com.eshop.app.repository.CategoryRepository;
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
 * Category seeder - Order 2.
 * Creates product categories.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class CategorySeeder implements Seeder<Category, SeederContext> {
    
    private final CategoryRepository categoryRepository;
    private final SeedProperties seedProperties;
    
    @Override
    public List<Category> seed(SeederContext context) {
        try {
            List<Category> categories = seedProperties.getCategories().stream()
                .map(this::buildCategory)
                .toList();
            
            List<Category> savedCategories = categoryRepository.saveAll(categories);
            
            // Populate context
            context.setCategories(savedCategories.stream()
                .collect(Collectors.toMap(Category::getName, Function.identity(),
                    (existing, replacement) -> {
                        log.warn("Duplicate category name: {}, keeping first", existing.getName());
                        return existing;
                    })));
            
            log.info("Seeded {} categories successfully", savedCategories.size());
            return savedCategories;
            
        } catch (DataAccessException e) {
            throw new CatalogSeedingException(
                "Failed to seed categories: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void cleanup() {
        try {
            categoryRepository.deleteAllInBatch();
            log.debug("Cleaned up existing categories");
        } catch (Exception e) {
            log.warn("Failed to cleanup categories: {}", e.getMessage());
        }
    }
    
    @Override
    public int order() {
        return 2;
    }
    
    @Override
    public String name() {
        return "CategorySeeder";
    }
    
    private Category buildCategory(SeedProperties.CategorySeed cfg) {
        return Category.builder()
            .name(cfg.getName())
            .description(cfg.getDescription())
            .imageUrl(cfg.getImageUrl())
            .active(true)
            .build();
    }
}
