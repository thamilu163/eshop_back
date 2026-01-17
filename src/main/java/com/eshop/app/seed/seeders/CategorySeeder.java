package com.eshop.app.seed.seeders;

import com.eshop.app.entity.Category;
import com.eshop.app.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Category Seeder.
 * Runs on application startup to populate category hierarchy.
 * Replaces previous config-based mechanism with code-first hierarchical seeding.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class CategorySeeder implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() > 0) {
            log.info("Categories already seeded. Skipping.");
            return;
        }

        log.info("Starting Category Seeding...");

        try {
            // Fashion & Apparel
            seedCategory("Fashion & Apparel", List.of(
                node("Men", "T-Shirts", "Shirts", "Jeans & Trousers", "Ethnic Wear", "Innerwear & Sleepwear"),
                node("Women", "Tops & T-Shirts", "Dresses", "Sarees & Kurtis", "Jeans & Leggings", "Innerwear"),
                node("Kids", "Boys Clothing", "Girls Clothing", "Infant Wear"),
                node("Footwear", "Casual Shoes", "Sports Shoes", "Formal Shoes", "Sandals & Slippers"),
                node("Accessories", "Bags & Wallets", "Belts", "Watches", "Sunglasses", "Jewelry")
            ));

            // Electronics
            seedCategory("Electronics", List.of(
                node("Mobiles & Accessories", "Smartphones", "Feature Phones", "Cases & Covers", "Chargers & Cables", "Power Banks"),
                node("Computers", "Laptops", "Desktops", "Monitors", "Keyboards & Mouse", "Storage Devices"),
                node("TV & Appliances", "Smart TVs", "Set-top Boxes", "Speakers", "Soundbars"),
                node("Cameras", "DSLR", "Mirrorless", "Action Cameras"),
                node("Smart Devices", "Smart Watches", "Fitness Bands", "Smart Home Devices")
            ));

            // Home & Living
            seedCategory("Home & Living", List.of(
                node("Furniture", "Sofa", "Beds", "Tables & Chairs", "Wardrobes"),
                node("Home Décor", "Wall Art", "Lamps & Lighting", "Clocks", "Showpieces"),
                node("Kitchen & Dining", "Cookware", "Dinner Sets", "Kitchen Tools"),
                node("Home Improvement", "Electricals", "Plumbing", "Hardware Tools")
            ));

            // Beauty, Health & Personal Care
            seedCategory("Beauty, Health & Personal Care", List.of(
                node("Beauty", "Makeup", "Skincare", "Haircare", "Personal Care"),
                node("Grooming", "Hygiene Products"),
                node("Health", "Supplements", "Medical Equipment", "Fitness Monitoring Devices"),
                node("Fragrances", "Perfumes", "Deodorants")
            ));

            // Grocery & Essentials (Detailed)
            seedCategory("Grocery & Essentials", List.of(
                node("Food", "Rice, Wheat & Pulses", "Snacks & Packaged Foods", "Spices"),
                node("Beverages", "Tea & Coffee", "Soft Drinks", "Health Drinks"),
                node("Household Essentials", "Cleaning Supplies", "Detergents", "Paper Products"),
                node("Fruits & Vegetables", 
                    node("Fruits", "Fresh Fruits", "Apples", "Bananas", "Oranges", "Mangoes", "Grapes", "Pomegranates", "Papaya", "Pineapple", "Watermelon", "Seasonal Fruits", "Exotic Fruits", "Kiwi", "Dragon Fruit", "Avocado", "Blueberries", "Dry Fruits", "Almonds", "Cashews", "Pistachios", "Raisins", "Dates", "Cut & Processed Fruits", "Fresh-cut Fruits", "Frozen Fruits"),
                    node("Vegetables", "Leafy Vegetables", "Spinach", "Lettuce", "Fenugreek", "Coriander", "Root Vegetables", "Potato", "Onion", "Carrot", "Beetroot", "Gourds & Pods", "Tomato", "Brinjal", "Lady Finger", "Bottle Gourd", "Beans", "Cruciferous Vegetables", "Cabbage", "Cauliflower", "Broccoli", "Exotic Vegetables", "Zucchini", "Bell Peppers", "Asparagus", "Frozen & Processed Vegetables", "Frozen Veg Mix", "Cut Vegetables"),
                    node("Organic & Special", "Organic Fruits", "Organic Vegetables", "Pesticide-Free Produce", "Hydroponic Produce")
                ),
                leaf("Dairy")
            ));

            // Sports, Fitness & Outdoor
            seedCategory("Sports, Fitness & Outdoor", List.of(
                node("Sports Equipment", "Cricket", "Football", "Badminton"),
                node("Fitness", "Gym Equipment", "Yoga Accessories"),
                node("Outdoor", "Camping Gear", "Trekking Equipment", "Cycling Accessories")
            ));

            // Toys, Kids & Baby
            seedCategory("Toys, Kids & Baby", List.of(
                node("Toys", "Educational Toys", "Action Figures", "Board Games"),
                node("Baby Care", "Diapers", "Baby Food", "Baby Grooming"),
                node("Kids Essentials", "School Bags", "Stationery")
            ));

            // Books, Office & Stationery
            seedCategory("Books, Office & Stationery", List.of(
                node("Books", "Academic", "Fiction", "Non-Fiction", "Competitive Exams"),
                node("Stationery", "Pens & Notebooks", "Art Supplies"),
                node("Office Supplies", "Printers", "Office Furniture")
            ));

            // Automotive
            seedCategory("Automotive", List.of(
                node("Vehicle Accessories", "Seat Covers", "Helmets", "Car Electronics"),
                node("Spare Parts", "Engine Parts", "Tyres", "Batteries"),
                node("Tools", "Car Care Tools", "Repair Kits")
            ));

            // Industrial & B2B
            seedCategory("Industrial & B2B", List.of(
                node("Machinery", "Manufacturing Equipment"),
                node("Electricals", "Switches", "Wires & Cables"),
                node("Safety", "Helmets", "Gloves", "Industrial Shoes"),
                node("Packaging", "Boxes", "Labels")
            ));

            // Digital Products
            seedCategory("Digital Products", List.of(
                node("Software", "Antivirus", "Business Tools"),
                node("Digital Content", "E-books", "Music"),
                node("Online Services", "Subscriptions", "Cloud Services"),
                node("Courses", "Programming", "Design", "Marketing")
            ));

            // Luxury & Specialty
            seedCategory("Luxury & Specialty", List.of(
                node("Luxury Fashion", "Designer Wear"),
                node("Jewelry", "Gold", "Diamond"),
                node("Collectibles", "Art", "Antiques"),
                node("Handmade", "Crafts", "Custom Products")
            ));

            // Services
            seedCategory("Services", "Repairs & Maintenance", "Home Services", "Freelancing Services", "Event Services");

            log.info("Category seeding completed successfully.");
        } catch (Exception e) {
            log.error("Failed to seed categories: {}", e.getMessage(), e);
            throw new RuntimeException("Category seeding failed", e);
        }
    }

    // --- Helpers ---

    private void seedCategory(String name, String... subCategories) {
        seedCategory(name, Arrays.stream(subCategories).map(this::leaf).toList());
    }

    private void seedCategory(String name, List<Node> children) {
        Category parent = createOrGet(name, null);
        for (Node child : children) {
            processNode(child, parent);
        }
    }

    private void processNode(Node node, Category parent) {
        Category category = createOrGet(node.name, parent);
        for (Node child : node.children) {
            processNode(child, category);
        }
    }

    private Category createOrGet(String name, Category parent) {
        return categoryRepository.findByName(name).orElseGet(() -> {
            log.debug("Creating category: {}", name);
            Category c = Category.builder()
                .name(name)
                .description(name)
                .active(true)
                .parent(parent)
                .build();
            return categoryRepository.save(c);
        });
    }

    // Helper data structures
    private record Node(String name, List<Node> children) {}

    private Node leaf(String name) {
        return new Node(name, Collections.emptyList());
    }

    private Node node(String name, String... children) {
        return new Node(name, Arrays.stream(children).map(this::leaf).toList());
    }
    
    // For nested structure
    private Node node(String name, Node... children) {
        return new Node(name, Arrays.asList(children));
    }
}
