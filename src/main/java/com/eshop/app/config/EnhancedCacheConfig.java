package com.eshop.app.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Enhanced caching configuration with Caffeine.
 * 
 * <p>Features:
 * <ul>
 *   <li>Per-cache TTL configuration based on data volatility</li>
 *   <li>Size-based eviction to prevent memory leaks</li>
 *   <li>Time-based expiration (write and access)</li>
 *   <li>Statistics recording for monitoring</li>
 *   <li>Weak key references for GC-friendly caching</li>
 * </ul>
 * 
 * <p>Cache Strategy:
 * <ul>
 *   <li>Short-lived (2-5min): Frequently changing data (products, orders)</li>
 *   <li>Medium-lived (15-30min): Semi-static data (categories, brands)</li>
 *   <li>Long-lived (1hr+): Rarely changing data (users, settings)</li>
 *   <li>Very short (1min): Real-time data (dashboards, analytics)</li>
 * </ul>
 * 
 * @author E-Shop Team
 * @version 2.0.0
 */
@Configuration
@EnableCaching
@Slf4j
public class EnhancedCacheConfig {

    @Value("${app.cache.products.ttl:300}")
    private int productCacheTtl;
    
    @Value("${app.cache.categories.ttl:1800}")
    private int categoryCacheTtl;
    
    @Value("${app.cache.dashboard.ttl:60}")
    private int dashboardCacheTtl;
    
    @Value("${app.cache.statistics.ttl:300}")
    private int statisticsCacheTtl;
    
    @Value("${app.cache.analytics.ttl:600}")
    private int analyticsCacheTtl;

    /**
     * Custom cache manager with per-cache configuration.
     */
    @Bean("enhancedCacheManager")
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager enhancedCacheManager() {
        log.info("Initializing custom Caffeine cache manager");
        
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        
        List<CaffeineCache> caches = Arrays.asList(
            // Products - Short TTL due to frequent updates
            buildCache("products", 500, productCacheTtl, TimeUnit.SECONDS),
            buildCache("productsList", 100, productCacheTtl, TimeUnit.SECONDS),
            buildCache("featuredProducts", 50, productCacheTtl, TimeUnit.SECONDS),
            buildCache("productDetails", 200, productCacheTtl, TimeUnit.SECONDS),
            
            // Categories - Medium TTL, rarely change
            buildCache("categories", 100, categoryCacheTtl, TimeUnit.SECONDS),
            buildCache("categoriesList", 50, categoryCacheTtl, TimeUnit.SECONDS),
            buildCache("categoryTree", 20, categoryCacheTtl, TimeUnit.SECONDS),
            
            // Brands - Medium TTL
            buildCache("brands", 100, 1800, TimeUnit.SECONDS),
            buildCache("brandsList", 50, 1800, TimeUnit.SECONDS),
            
            // Users - Long TTL, updated infrequently
            buildCache("users", 1000, 3600, TimeUnit.SECONDS),
            buildCache("userProfiles", 500, 3600, TimeUnit.SECONDS),
            
            // Shops - Medium to long TTL
            buildCache("shops", 500, 1800, TimeUnit.SECONDS),
            buildCache("shopsList", 100, 1800, TimeUnit.SECONDS),
            
            // Orders - Short TTL due to status changes
            buildCache("orders", 200, 300, TimeUnit.SECONDS),
            buildCache("orderDetails", 100, 300, TimeUnit.SECONDS),
            
            // Dashboard & Analytics - Very short TTL for freshness
            buildCache("adminDashboard", 10, dashboardCacheTtl, TimeUnit.SECONDS),
            buildCache("sellerDashboard", 50, dashboardCacheTtl, TimeUnit.SECONDS),
            buildCache("adminStatistics", 20, statisticsCacheTtl, TimeUnit.SECONDS),
            buildCache("sellerStatistics", 50, statisticsCacheTtl, TimeUnit.SECONDS),
            buildCache("adminAnalytics", 20, analyticsCacheTtl, TimeUnit.SECONDS),
            buildCache("sellerAnalytics", 50, analyticsCacheTtl, TimeUnit.SECONDS),
            buildCache("productStatistics", 100, statisticsCacheTtl, TimeUnit.SECONDS),
            
            // Reviews - Medium TTL
            buildCache("reviews", 200, 900, TimeUnit.SECONDS),
            buildCache("reviewsList", 100, 900, TimeUnit.SECONDS),
            
            // Coupons - Medium TTL
            buildCache("coupons", 100, 1800, TimeUnit.SECONDS),
            
            // Shipping - Long TTL, rarely changes
            buildCache("shippingRates", 50, 3600, TimeUnit.SECONDS),
            
            // Inventory - Short TTL due to stock changes
            buildCache("inventory", 500, 180, TimeUnit.SECONDS)
        );
        
        cacheManager.setCaches(caches);
        
        log.info("Initialized {} Caffeine caches with custom TTLs", caches.size());
        
        return cacheManager;
    }

    /**
     * Builds a Caffeine cache with specified parameters.
     *
     * @param name cache name
     * @param maxSize maximum number of entries
     * @param duration expiration duration
     * @param timeUnit time unit for duration
     * @return configured CaffeineCache
     */
    private CaffeineCache buildCache(String name, int maxSize, long duration, TimeUnit timeUnit) {
        log.debug("Building cache '{}': maxSize={}, duration={}{}",
            name, maxSize, duration, timeUnit.name().toLowerCase());
        
        return new CaffeineCache(
            name,
            Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(duration, timeUnit)
                .expireAfterAccess(duration * 2, timeUnit)  // Grace period for frequently accessed items
                .recordStats()  // Enable statistics for monitoring
                .weakKeys()     // Allow GC to collect keys
                .build()
        );
    }
    
    /**
     * Default Caffeine specification for backwards compatibility.
     */
    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats();
    }
}
