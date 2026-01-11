package com.eshop.app.service;

import com.eshop.app.dto.request.SellerProfileUpdateRequest;
import com.eshop.app.dto.request.SellerRegisterRequest;
import com.eshop.app.dto.response.SellerProfileResponse;
import com.eshop.app.entity.SellerProfile;
import com.eshop.app.entity.User;
import com.eshop.app.exception.ValidationException;
import com.eshop.app.exception.ResourceNotFoundException;
import com.eshop.app.repository.SellerProfileRepository;
import com.eshop.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerService {
    
    private final SellerProfileRepository sellerProfileRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public SellerProfileResponse registerSeller(Long userId, SellerRegisterRequest request) {
        log.info("Registering seller profile for userId: {}", userId);
        
        // Check if profile already exists
        if (sellerProfileRepository.existsByUserId(userId)) {
            throw new ValidationException("User already has a seller profile");
        }
        
        // Validate terms acceptance
        if (!request.isAcceptedTerms()) {
            throw new ValidationException("Terms must be accepted");
        }
        
        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Create profile
        SellerProfile profile = SellerProfile.builder()
                .user(user)
                .sellerType(request.getSellerType())
                .displayName(request.getDisplayName())
                .businessName(request.getBusinessName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .taxId(request.getTaxId())
                .description(request.getDescription())
                .status(SellerProfile.SellerStatus.ACTIVE)
                // Legacy fields
                .aadhar(request.getAadhar())
                .pan(request.getPan())
                .gstin(request.getGstin())
                .businessType(request.getBusinessType())
                .shopName(request.getShopName())
                .farmLocationVillage(request.getFarmLocationVillage())
                .landArea(request.getLandArea())
                .warehouseLocation(request.getWarehouseLocation())
                .bulkPricingAgreement(request.getBulkPricingAgreement())
                .build();
        
        SellerProfile saved = sellerProfileRepository.save(profile);
        log.info("Seller profile created with id: {} for userId: {}", saved.getId(), userId);
        
        return toResponse(saved);
    }
    
    @Transactional(readOnly = true)
    public SellerProfileResponse getSellerProfile(Long userId) {
        log.debug("Fetching seller profile for userId: {}", userId);
        
        SellerProfile profile = sellerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found for userId: " + userId));
        
        return toResponse(profile);
    }
    
    @Transactional(readOnly = true)
    public boolean hasProfile(Long userId) {
        return sellerProfileRepository.existsByUserId(userId);
    }
    
    @Transactional
    public SellerProfileResponse updateSellerProfile(Long userId, SellerProfileUpdateRequest request) {
        log.info("Updating seller profile for userId: {}", userId);
        
        SellerProfile profile = sellerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found for userId: " + userId));
        
        profile.setSellerType(request.getSellerType());
        profile.setDisplayName(request.getDisplayName());
        profile.setBusinessName(request.getBusinessName());
        profile.setEmail(request.getEmail());
        profile.setPhone(request.getPhone());
        profile.setTaxId(request.getTaxId());
        profile.setDescription(request.getDescription());
        // Legacy fields
        profile.setAadhar(request.getAadhar());
        profile.setPan(request.getPan());
        profile.setGstin(request.getGstin());
        profile.setBusinessType(request.getBusinessType());
        profile.setShopName(request.getShopName());
        profile.setFarmLocationVillage(request.getFarmLocationVillage());
        profile.setLandArea(request.getLandArea());
        profile.setWarehouseLocation(request.getWarehouseLocation());
        profile.setBulkPricingAgreement(request.getBulkPricingAgreement());
        
        SellerProfile updated = sellerProfileRepository.save(profile);
        log.info("Seller profile updated for userId: {}", userId);
        
        return toResponse(updated);
    }
    
    /**
     * Resolve user ID from authentication (supports both JWT and PrincipalDetails)
     */
    public Long resolveUserId(Authentication authentication) {
        if (authentication == null) {
            throw new ValidationException("Authentication required");
        }
        
        Object principal = authentication.getPrincipal();
        
        // Try PrincipalDetails first
        if (principal instanceof com.eshop.app.config.EnhancedSecurityConfig.PrincipalDetails pd) {
            return pd.getId();
        }
        
        // Try JWT
        if (authentication instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtToken) {
            org.springframework.security.oauth2.jwt.Jwt jwt = jwtToken.getToken();
            Object userIdClaim = jwt.getClaim("user_id");
            if (userIdClaim != null) {
                if (userIdClaim instanceof Number) {
                    return ((Number) userIdClaim).longValue();
                }
                if (userIdClaim instanceof String) {
                    return Long.parseLong((String) userIdClaim);
                }
            }
        }
        
        throw new ValidationException("Unable to resolve user ID from authentication");
    }
    
    private SellerProfileResponse toResponse(SellerProfile profile) {
        return SellerProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser() != null ? profile.getUser().getId() : null)
                .sellerType(profile.getSellerType())
                .displayName(profile.getDisplayName())
                .businessName(profile.getBusinessName())
                .email(profile.getEmail())
                .phone(profile.getPhone())
                .taxId(profile.getTaxId())
                .description(profile.getDescription())
                .status(profile.getStatus())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                // Legacy fields
                .aadhar(profile.getAadhar())
                .pan(profile.getPan())
                .gstin(profile.getGstin())
                .businessType(profile.getBusinessType())
                .shopName(profile.getShopName())
                .farmLocationVillage(profile.getFarmLocationVillage())
                .landArea(profile.getLandArea())
                .warehouseLocation(profile.getWarehouseLocation())
                .bulkPricingAgreement(profile.getBulkPricingAgreement())
                .build();
    }
}
