package com.eshop.app.dto.response;

import com.eshop.app.entity.SellerProfile;
import com.eshop.app.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerProfileResponse {
    private Long id;
    private Long userId;
    private User.SellerType sellerType;
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
    private String shopName;
    private String farmLocationVillage;
    private String landArea;
    private String warehouseLocation;
    private Boolean bulkPricingAgreement;
}
