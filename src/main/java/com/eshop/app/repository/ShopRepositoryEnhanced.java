package com.eshop.app.repository;

import com.eshop.app.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * Enhanced ShopRepository with statistics queries.
 * 
 * @author EShop Team
 * @since 2.0
 */
@Repository
public interface ShopRepositoryEnhanced extends JpaRepository<Shop, Long> {
    
    /**
     * Gets aggregated shop statistics.
     * 
     * @return map containing shop counts
     */
    @Query("""
        SELECT new map(
            COUNT(s.id) as totalShops,
            COUNT(CASE WHEN s.active = true THEN 1 END) as activeShops
        )
        FROM Shop s
        WHERE s.deleted = false
        """)
    Map<String, Object> getShopStatistics();
}
