package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "shops", indexes = {
    @Index(name = "idx_shop_name", columnList = "shop_name"),
    @Index(name = "idx_shop_seller", columnList = "seller_id"),
    @Index(name = "idx_shop_location", columnList = "latitude,longitude"),
    @Index(name = "idx_shop_city", columnList = "city"),
    @Index(name = "idx_shop_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(callSuper = true)
public class Shop extends BaseEntity {
    
    @Column(name = "shop_name", nullable = false, unique = true, length = 200)
    private String shopName;
    
    @Column(nullable = false, length = 1000)
    private String description;
    
    @Column(length = 500)
    private String address;
    
    @Column(length = 20)
    private String phone;
    
    @Column(length = 150)
    private String email;
    
    @Column(name = "logo_url", length = 500)
    private String logoUrl;
    
    // Geospatial fields for location-based search
    @Column
    private Double latitude;
    
    @Column
    private Double longitude;
    
    @Column(length = 100)
    private String city;
    
    @Column(length = 100)
    private String state;
    
    @Column(length = 100)
    private String country;
    
    @Column(name = "postal_code", length = 20)
    private String postalCode;
    
    @Column(name = "place_id", length = 200)
    private String placeId; // Google Place ID
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @Column
    private Double rating; // Shop rating (0-5)
    
    @Enumerated(EnumType.STRING)
    @Column(name = "seller_type", length = 20)
    private User.SellerType sellerType;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false, unique = true)
    private User seller;
    
    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private final Set<Product> products = new HashSet<>();
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    // Explicit getter for shopName (required by some services)
    public String getShopName() {
        return this.shopName;
    }

    // Explicit getter for seller (required by some services)
    public User getSeller() {
        return this.seller;
    }
}
