package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seller_profiles", indexes = {
    @Index(name = "idx_seller_profile_user_id", columnList = "user_id"),
    @Index(name = "idx_seller_profile_status", columnList = "status"),
    @Index(name = "idx_seller_profile_seller_type", columnList = "seller_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerProfile extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "seller_type", nullable = false, length = 20)
    private User.SellerType sellerType;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "business_name", length = 200)
    private String businessName;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SellerStatus status = SellerStatus.ACTIVE;

    // Legacy fields for backward compatibility
    @Column(length = 50)
    private String aadhar;

    @Column(length = 50)
    private String pan;

    @Column(length = 50)
    private String gstin;

    @Column(length = 150)
    private String businessType;

    @Column(length = 150)
    private String shopName;

    @Column(length = 150)
    private String farmLocationVillage;

    @Column(length = 50)
    private String landArea;

    @Column(length = 255)
    private String warehouseLocation;

    @Column(name = "bulk_pricing_agreement")
    @Builder.Default
    private Boolean bulkPricingAgreement = false;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    public enum SellerStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED
    }
}
