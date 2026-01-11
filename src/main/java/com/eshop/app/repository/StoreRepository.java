package com.eshop.app.repository;

import com.eshop.app.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByStoreName(String storeName);

    Optional<Store> findBySellerId(Long sellerId);

    Optional<Store> findByDomain(String domain);

    List<Store> findByActiveTrue();

    boolean existsByDomain(String domain);

    Page<Store> findByActive(Boolean active, Pageable pageable);

    @Query("SELECT s FROM Store s WHERE LOWER(s.storeName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Store> searchStores(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = """
        SELECT s.*, 
            (6371 * acos(
                cos(radians(:latitude)) * cos(radians(sl.latitude)) *
                cos(radians(sl.longitude) - radians(:longitude)) +
                sin(radians(:latitude)) * sin(radians(sl.latitude))
            )) AS distance_km
        FROM stores s
        INNER JOIN shop_locations sl ON s.location_id = sl.id
        WHERE s.active = true
        AND sl.latitude IS NOT NULL 
        AND sl.longitude IS NOT NULL
        HAVING distance_km <= :radiusKm
        ORDER BY distance_km ASC
        """, nativeQuery = true)
    List<Object[]> findNearbyStores(
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("radiusKm") Integer radiusKm
    );
}
