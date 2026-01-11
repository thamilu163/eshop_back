package com.eshop.app.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * Externalized seed data configuration for development/testing.
 * Populate `app.seed.*` in application-dev.properties or environment-specific config.
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app.seed")
public class SeedProperties {

    /** Enable/disable seeding (default: true for dev profile) */
    private boolean enabled = true;

    /** Users to seed */
    @Valid
    private List<UserSeed> users = new ArrayList<>();

    /** Categories to seed */
    @Valid
    private List<CategorySeed> categories = new ArrayList<>();

    /** Brands to seed */
    @Valid
    private List<BrandSeed> brands = new ArrayList<>();

    /** Tags to seed */
    @Valid
    private List<TagSeed> tags = new ArrayList<>();

    /** Shops to seed */
    @Valid
    private List<ShopSeed> shops = new ArrayList<>();

    /** Products to seed */
    @Valid
    private List<ProductSeed> products = new ArrayList<>();

    @Getter
    @Setter
    public static class UserSeed {
        @NotBlank
        private String username;
        private String email;
        private String password; // optional: if absent, DataSeeder will generate one or use env override
        private String firstName;
        private String lastName;
        private String phone;
        private String address;
        private String role; // ADMIN, SELLER, CUSTOMER, DELIVERY_AGENT
        private String sellerType; // INDIVIDUAL, BUSINESS, FARMER, WHOLESALER, RETAILER
    }

    @Getter
    @Setter
    public static class CategorySeed {
        @NotBlank
        private String name;
        private String description;
        private String imageUrl;
    }

    @Getter
    @Setter
    public static class BrandSeed {
        @NotBlank
        private String name;
        private String description;
        private String logoUrl;
    }

    @Getter
    @Setter
    public static class TagSeed {
        @NotBlank
        private String name;
    }

    @Getter
    @Setter
    public static class ShopSeed {
        @NotBlank
        private String shopName;
        private String description;
        private String address;
        private String phone;
        private String email;
        private String logoUrl;
        private String sellerUsername; // link to seeded user
        private String sellerType;
    }

    @Getter
    @Setter
    public static class ProductSeed {
        @NotBlank
        private String name;
        private String description;
        private String sku;
        private String price; // use string to allow placeholder resolution
        private String discountPrice;
        private Integer stockQuantity;
        private String imageUrl;
        private String categoryName;
        private String brandName;
        private String shopName;
        private List<String> tags = new ArrayList<>();
        private boolean featured = false;
        private boolean active = true;
    }
}
