package com.eshop.app.controller;

import com.eshop.app.dto.request.ShopCreateRequest;
import com.eshop.app.dto.response.ApiResponse;
import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.dto.response.ShopResponse;
import com.eshop.app.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.eshop.app.constants.ApiConstants;

@Tag(name = "Shops", description = "Multi-vendor shop management - sellers can create and manage their shops")
@RestController
@RequestMapping(ApiConstants.Endpoints.SHOPS)
public class ShopController {
    
    private final ShopService shopService;
    
    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(
        summary = "Create new shop",
        description = "Create a new shop. Available for SELLER and ADMIN roles.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<ShopResponse>> createShop(
            @Valid @RequestBody ShopCreateRequest request) {
        ShopResponse response = shopService.createShop(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shop created successfully", response));
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(
        summary = "Update shop",
        description = "Update shop information. Sellers can update their own shop, admins can update any shop.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<ShopResponse>> updateShop(
            @Parameter(description = "Shop ID") @PathVariable Long id,
            @Valid @RequestBody ShopCreateRequest request) {
        ShopResponse response = shopService.updateShop(id, request);
        return ResponseEntity.ok(ApiResponse.success("Shop updated successfully", response));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Delete shop (Admin only)",
        description = "Delete a shop. All products in this shop will also be removed.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<Void>> deleteShop(
            @Parameter(description = "Shop ID") @PathVariable Long id) {
        shopService.deleteShop(id);
        return ResponseEntity.ok(ApiResponse.success("Shop deleted successfully", null));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShopResponse>> getShopById(@PathVariable Long id) {
        ShopResponse response = shopService.getShopById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/my-shop")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<ShopResponse>> getMyShop() {
        ShopResponse response = shopService.getMyShop();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ShopResponse>>> getAllShops(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "shopName") String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        PageResponse<ShopResponse> response = shopService.getAllShops(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<ShopResponse>>> searchShops(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<ShopResponse> response = shopService.searchShops(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
