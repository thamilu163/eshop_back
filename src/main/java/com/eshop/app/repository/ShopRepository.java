package com.eshop.app.repository;

import com.eshop.app.entity.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {
    
    Optional<Shop> findByShopName(String shopName);
    
    Optional<Shop> findBySellerId(Long sellerId);
    
    boolean existsByShopName(String shopName);
    
    Page<Shop> findByActive(Boolean active, Pageable pageable);
    
    @Query("SELECT s FROM Shop s WHERE " +
           "LOWER(s.shopName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Shop> searchShops(@Param("keyword") String keyword, Pageable pageable);
}
