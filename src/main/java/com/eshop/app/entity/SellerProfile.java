package com.eshop.app.entity;

import com.eshop.app.enums.SellerIdentityType;
import com.eshop.app.enums.SellerBusinessType;
import java.util.Set;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seller_profiles", indexes = {
    @Index(name = "idx_seller_profile_user_id", columnList = "user_id"),
    @Index(name = "idx_seller_profile_status", columnList = "status"),
        @Index(name = "idx_seller_profile_identity_type", columnList = "identity_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerProfile extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_type", nullable = false, length = 20)
    private SellerIdentityType identityType;

    @ElementCollection(targetClass = SellerBusinessType.class)
    @CollectionTable(name = "seller_business_types", joinColumns = @JoinColumn(name = "seller_profile_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "business_type")
    private Set<SellerBusinessType> businessTypes;

    @Column(name = "is_own_produce")
    @Builder.Default
    private Boolean isOwnProduce = false;

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

    @Column(length = 150, name = "store_name")
    private String storeName;

    @Column(length = 150)
    private String farmLocationVillage;

    @Column(length = 50)
    private String landArea;

    @Column(length = 255)
    private String warehouseLocation;

    @Column(name = "bulk_pricing_agreement")
    @Builder.Default
    private Boolean bulkPricingAgreement = false;

    // New KYC Fields
    @Column(name = "authorized_signatory", length = 100)
    private String authorizedSignatory;

    @Column(name = "registration_proof", length = 500)
    private String registrationProof;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    public enum SellerStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED
    }
}
