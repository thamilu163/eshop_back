package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "stores", indexes = {
    @Index(name = "idx_store_name", columnList = "store_name"),
    @Index(name = "idx_store_seller", columnList = "seller_id"),
    @Index(name = "idx_store_location", columnList = "latitude,longitude"),
    @Index(name = "idx_store_city", columnList = "city"),
    @Index(name = "idx_store_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store extends BaseEntity {
    @Column(name = "store_name", nullable = false, unique = true, length = 200)
    private String storeName;

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

    @Column(length = 200)
    private String domain;

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
    private String placeId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column
    private Double rating;

    @Enumerated(EnumType.STRING)
    @Column(name = "seller_type", length = 20)
    private User.SellerType sellerType;

    @OneToOne
    @JoinColumn(name = "seller_id", nullable = false, unique = true)
    private User seller;

    // Products are linked via Shop/Product relationship and via ProductToStore mapping.
    // Removed direct OneToMany to Product to avoid mapping conflicts with existing Shop entity.
}