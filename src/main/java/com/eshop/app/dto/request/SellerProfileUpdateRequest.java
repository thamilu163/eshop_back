package com.eshop.app.dto.request;

import com.eshop.app.entity.User;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerProfileUpdateRequest {
    
    @NotNull(message = "Seller type is required")
    private User.SellerType sellerType;
    
    @NotBlank(message = "Display name is required")
    @Size(min = 2, max = 100, message = "Display name must be between 2 and 100 characters")
    private String displayName;
    
    @Size(min = 2, max = 200, message = "Business name must be between 2 and 200 characters")
    private String businessName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
    private String phone;
    
    @Size(min = 5, max = 50, message = "Tax ID must be between 5 and 50 characters")
    private String taxId;
    
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;
    
    // Legacy fields
    private String aadhar;
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Invalid PAN format")
    private String pan;
    
    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", message = "Invalid GSTIN format")
    private String gstin;
    private String businessType;
    private String storeName;
    private String farmLocationVillage;
    private String landArea;
    private String warehouseLocation;
    private Boolean bulkPricingAgreement;
}
