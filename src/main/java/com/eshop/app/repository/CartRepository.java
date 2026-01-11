package com.eshop.app.repository;

import com.eshop.app.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    
    Optional<Cart> findByUserId(Long userId);
    
    Optional<Cart> findByCartCode(String cartCode);
    
    boolean existsByCartCode(String cartCode);
    
    void deleteByUserId(Long userId);
    
    void deleteByCartCode(String cartCode);
}
