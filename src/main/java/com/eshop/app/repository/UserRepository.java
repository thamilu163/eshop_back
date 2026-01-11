package com.eshop.app.repository;

import com.eshop.app.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {})
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailForAuth(@Param("email") String email);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {})
    @Query("SELECT u FROM User u WHERE u.username = :username")
    Optional<User> findByUsernameForAuth(@Param("username") String username);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    Page<User> findByRole(User.UserRole role, Pageable pageable);
    
    Page<User> findByActive(Boolean active, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);
    
    // Dashboard Analytics Methods
    long countByRole(User.UserRole role);
    long countByActiveTrue();
    long countByCreatedAtAfter(java.time.LocalDateTime createdAt);
    
    @Query("SELECT DATE(u.createdAt) as date, COUNT(u) as count FROM User u GROUP BY DATE(u.createdAt) ORDER BY date DESC")
    java.util.List<java.util.Map<String, Object>> getUserGrowthData();
}
