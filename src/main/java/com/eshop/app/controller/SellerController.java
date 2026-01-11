package com.eshop.app.controller;

import com.eshop.app.constants.ApiConstants;
import com.eshop.app.dto.request.SellerProfileUpdateRequest;
import com.eshop.app.dto.request.SellerRegisterRequest;
import com.eshop.app.dto.response.ApiResponse;
import com.eshop.app.dto.response.SellerProfileResponse;
import com.eshop.app.service.SellerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Unified Seller Controller - Manages seller profiles for all seller types.
 * 
 * <p>Supports unified seller architecture with single SELLER role and seller profiles.</p>
 */
@Tag(
    name = "Seller Management", 
    description = "Unified seller profile management - Register, view, and update seller profiles"
)
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/sellers")
@RequiredArgsConstructor
@Slf4j
public class SellerController {
    
    private final SellerService sellerService;
    
    /**
     * Register a new seller profile.
     * 
     * <p>Creates a seller profile for the authenticated user with the specified seller type.
     * The user must have SELLER role and must not have an existing profile.</p>
     */
    @PostMapping("/register")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(
        summary = "Register seller profile",
        description = """
            Create a new seller profile for the authenticated user.
            
            **Requirements:**
            - User must have SELLER role
            - User must not have an existing profile
            - Terms must be accepted
            
            **Seller Types:**
            - INDIVIDUAL: Individual/small-scale seller
            - BUSINESS: Business entity seller
            - FARMER: Agricultural products seller
            - WHOLESALER: Bulk/wholesale seller
            - RETAILER: Retail products seller
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Seller profile created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Success",
                    value = """
                        {
                          "status": "success",
                          "message": "Seller profile registered successfully",
                          "data": {
                            "id": 1,
                            "userId": 123,
                            "sellerType": "FARMER",
                            "displayName": "Green Valley Farm",
                            "email": "contact@greenvalley.com",
                            "phone": "+919876543210",
                            "status": "ACTIVE",
                            "createdAt": "2026-01-11T10:30:00Z"
                          }
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - User already has a profile or validation failed",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "status": "error",
                          "message": "User already has a seller profile"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - SELLER role required"
        )
    })
    public ResponseEntity<ApiResponse<SellerProfileResponse>> registerSeller(
            @Parameter(
                description = "Seller registration request with profile details",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = SellerRegisterRequest.class),
                    examples = {
                        @ExampleObject(
                            name = "Farmer Seller",
                            value = """
                                {
                                  "sellerType": "FARMER",
                                  "displayName": "Green Valley Farm",
                                  "businessName": "Green Valley Organic Farms Pvt Ltd",
                                  "email": "contact@greenvalley.com",
                                  "phone": "+919876543210",
                                  "description": "Organic vegetables and fruits",
                                  "acceptedTerms": true,
                                  "aadhar": "123456789012",
                                  "farmLocationVillage": "Pune",
                                  "landArea": "10 acres"
                                }
                                """
                        ),
                        @ExampleObject(
                            name = "Business Seller",
                            value = """
                                {
                                  "sellerType": "BUSINESS",
                                  "displayName": "TechHub Electronics",
                                  "businessName": "TechHub Electronics Pvt Ltd",
                                  "email": "sales@techhub.com",
                                  "phone": "+911234567890",
                                  "taxId": "GSTIN123456",
                                  "description": "Electronics and gadgets retailer",
                                  "acceptedTerms": true
                                }
                                """
                        )
                    }
                )
            )
            @Valid @RequestBody SellerRegisterRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        
        Long userId = sellerService.resolveUserId(authentication);
        log.info("Seller registration request for userId: {}, sellerType: {}", userId, request.getSellerType());
        
        SellerProfileResponse response = sellerService.registerSeller(userId, request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Seller profile registered successfully", response));
    }
    
    /**
     * Get authenticated seller's profile.
     * 
     * <p>Retrieves the complete profile information for the authenticated seller.</p>
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(
        summary = "Get seller profile",
        description = """
            Retrieve the authenticated seller's complete profile information.
            
            **Returns:**
            - Profile details including seller type, contact information, and status
            - Business/legacy fields if available
            - Creation and update timestamps
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Profile retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": "success",
                          "data": {
                            "id": 1,
                            "userId": 123,
                            "sellerType": "FARMER",
                            "displayName": "Green Valley Farm",
                            "businessName": "Green Valley Organic Farms Pvt Ltd",
                            "email": "contact@greenvalley.com",
                            "phone": "+919876543210",
                            "taxId": null,
                            "description": "Organic vegetables and fruits",
                            "status": "ACTIVE",
                            "createdAt": "2026-01-10T10:30:00Z",
                            "updatedAt": "2026-01-11T14:20:00Z",
                            "aadhar": "123456789012",
                            "farmLocationVillage": "Pune",
                            "landArea": "10 acres"
                          }
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - SELLER role required"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Profile not found",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "status": "error",
                          "message": "Seller profile not found for userId: 123"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<ApiResponse<SellerProfileResponse>> getProfile(
            @Parameter(hidden = true) Authentication authentication) {
        
        Long userId = sellerService.resolveUserId(authentication);
        log.debug("Fetching seller profile for userId: {}", userId);
        
        SellerProfileResponse response = sellerService.getSellerProfile(userId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Update authenticated seller's profile.
     * 
     * <p>Updates the seller profile information. All fields except acceptedTerms can be updated.</p>
     */
    @PutMapping("/profile")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(
        summary = "Update seller profile",
        description = """
            Update the authenticated seller's profile information.
            
            **Updatable Fields:**
            - Seller type
            - Display name
            - Business name
            - Contact information (email, phone)
            - Tax ID
            - Description
            - Legacy fields (aadhar, pan, gstin, etc.)
            
            **Note:** Status cannot be updated by the seller.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Profile updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": "success",
                          "message": "Seller profile updated successfully",
                          "data": {
                            "id": 1,
                            "userId": 123,
                            "sellerType": "FARMER",
                            "displayName": "Green Valley Organic Farm",
                            "email": "newemail@greenvalley.com",
                            "phone": "+919876543211",
                            "status": "ACTIVE",
                            "updatedAt": "2026-01-11T15:45:00Z"
                          }
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - Validation failed"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - SELLER role required"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Profile not found"
        )
    })
    public ResponseEntity<ApiResponse<SellerProfileResponse>> updateProfile(
            @Parameter(
                description = "Seller profile update request",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = SellerProfileUpdateRequest.class),
                    examples = @ExampleObject(
                        value = """
                            {
                              "sellerType": "FARMER",
                              "displayName": "Green Valley Organic Farm",
                              "businessName": "Green Valley Organic Farms Pvt Ltd",
                              "email": "newemail@greenvalley.com",
                              "phone": "+919876543211",
                              "description": "Premium organic vegetables, fruits, and dairy products"
                            }
                            """
                    )
                )
            )
            @Valid @RequestBody SellerProfileUpdateRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        
        Long userId = sellerService.resolveUserId(authentication);
        log.info("Updating seller profile for userId: {}", userId);
        
        SellerProfileResponse response = sellerService.updateSellerProfile(userId, request);
        
        return ResponseEntity.ok(ApiResponse.success("Seller profile updated successfully", response));
    }
    
    /**
     * Check if authenticated user has a seller profile.
     * 
     * <p>Quick check to determine if the seller needs to complete profile registration.</p>
     */
    @GetMapping("/profile/exists")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(
        summary = "Check if seller profile exists",
        description = """
            Check if the authenticated user has completed seller profile registration.
            
            **Use Cases:**
            - Pre-flight check before accessing seller features
            - Redirect to registration if profile doesn't exist
            - UI conditional rendering
            
            **Returns:**
            - `true`: Profile exists and seller can access features
            - `false`: Profile doesn't exist, registration required
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Check completed successfully",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "Profile Exists",
                        value = """
                            {
                              "status": "success",
                              "data": true
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Profile Not Found",
                        value = """
                            {
                              "status": "success",
                              "data": false
                            }
                            """
                    )
                }
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - SELLER role required"
        )
    })
    public ResponseEntity<ApiResponse<Boolean>> checkProfileExists(
            @Parameter(hidden = true) Authentication authentication) {
        
        Long userId = sellerService.resolveUserId(authentication);
        boolean exists = sellerService.hasProfile(userId);
        
        return ResponseEntity.ok(ApiResponse.success(exists));
    }
}
