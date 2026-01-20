package com.eshop.app.dto.response;

import com.eshop.app.entity.SellerProfile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.eshop.app.enums.SellerIdentityType;
import com.eshop.app.enums.SellerBusinessType;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerProfileResponse {
    private Long id;
    private Long userId;
    private SellerIdentityType identityType;
    private Set<SellerBusinessType> businessTypes;
    private Boolean isOwnProduce;
    private String displayName;
    private String businessName;
    private String email;
    private String phone;
    private String taxId;
    private String description;
    private SellerProfile.SellerStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Legacy fields
    private String aadhar;
    private String pan;
    private String gstin;
    private String businessType;
    private String storeName;
    private String farmLocationVillage;
    private String landArea;

    private String warehouseLocation;
    private Boolean bulkPricingAgreement;

    // KYC Fields
    private String authorizedSignatory;
    private String registrationProof;
}
