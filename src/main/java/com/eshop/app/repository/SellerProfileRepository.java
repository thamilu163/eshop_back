package com.eshop.app.repository;

import com.eshop.app.entity.SellerProfile;
import com.eshop.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerProfileRepository extends JpaRepository<SellerProfile, Long> {
    
    Optional<SellerProfile> findByUser_Id(Long userId);
    
    boolean existsByUser_Id(Long userId);
    
    @Query("SELECT COUNT(sp) FROM SellerProfile sp WHERE sp.sellerType = :type")
    long countBySellerType(@Param("type") User.SellerType type);
    
    @Query("SELECT sp FROM SellerProfile sp WHERE sp.user.email = :email")
    Optional<SellerProfile> findByUserEmail(@Param("email") String email);
}
