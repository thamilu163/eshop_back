package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_username", columnList = "username")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = { "cart", "store", "orders", "deliveryAgentProfile", "sellerProfile" })
@EqualsAndHashCode(callSuper = true, of = {"email"})
public class User extends BaseEntity {
    
    @Column(nullable = false, unique = true, length = 100)
    private String username;
    
    @Column(nullable = false, unique = true, length = 150)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(name = "first_name", nullable = true, length = 100)
    private String firstName;
    
    @Column(name = "last_name", nullable = true, length = 100)
    private String lastName;
    
    @Column(length = 20)
    private String phone;
    
    @Column(length = 500)
    private String address;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "seller_type", length = 20)
    private SellerType sellerType;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = false;
    
    @Column(name = "two_factor_enabled")
    @Builder.Default
    private Boolean twoFactorEnabled = false;

    @Column(name = "two_factor_secret", length = 500)
    private String twoFactorSecret;
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Cart cart;
    
    @OneToOne(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Store store;
    
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Order> orders = new HashSet<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private DeliveryAgentProfile deliveryAgentProfile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private SellerProfile sellerProfile;
    
    public enum UserRole {
        ADMIN,
        SELLER,
        CUSTOMER,
        DELIVERY_AGENT
    }
    
    public enum SellerType {
        INDIVIDUAL,       // Individual seller (small scale)
        BUSINESS,         // Business entity (replaces SHOP)
        FARMER,           // Farm-grown products seller
        WHOLESALER,       // Wholesale/bulk sales
        RETAILER          // Retail sales (replaces RETAIL_SELLER)
    }
    // Explicit getter for compatibility
    public String getUsername() {
        return this.username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    // Explicit getters for compatibility
    public UserRole getRole() {
        return this.role;
    }
    public Boolean getActive() {
        return this.active;
    }
    public String getEmail() {
        return this.email;
    }
    public String getPassword() {
        return this.password;
    }
}
