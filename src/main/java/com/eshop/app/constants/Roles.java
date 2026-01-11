package com.eshop.app.constants;

/**
 * Role Constants for Authorization
 * 
 * <p>Centralized role definitions to prevent typos and maintain consistency
 * across the application. Use these constants in @PreAuthorize annotations.</p>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>
 * {@code @PreAuthorize("hasRole(T(com.eshop.app.constants.Roles).SELLER)")}
 * public ResponseEntity<?> getSellerDashboard() { ... }
 * </pre>
 * 
 * @author EShop Team
 * @version 1.0
 * @since 2026-01-01
 */
public final class Roles {
    
    private Roles() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }
    
    /**
     * Administrator role - full system access
     */
    public static final String ADMIN = "ADMIN";
    
    /**
     * Seller role - can manage products, shops, and orders
     */
    public static final String SELLER = "SELLER";
    
    /**
     * Customer role - can browse, purchase, and manage orders
     */
    public static final String CUSTOMER = "CUSTOMER";
    
    /**
     * Delivery agent role - can manage deliveries
     */
    public static final String DELIVERY_AGENT = "DELIVERY_AGENT";
}
