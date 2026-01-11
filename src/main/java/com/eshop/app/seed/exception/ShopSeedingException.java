package com.eshop.app.seed.exception;

/**
 * Exception thrown when shop seeding fails.
 */
public final class ShopSeedingException extends SeedingException {
    
    public ShopSeedingException(String message, Throwable cause) {
        super(message, cause, SeedPhase.SHOP_SEEDING);
    }
    
    public ShopSeedingException(String message) {
        super(message, SeedPhase.SHOP_SEEDING);
    }
}
