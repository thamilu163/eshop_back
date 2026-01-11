-- Unified Seller Architecture Migration
-- This migration consolidates seller types and enhances the seller_profiles table

-- Step 1: Add new columns to seller_profiles table if they don't exist
ALTER TABLE seller_profiles ADD COLUMN IF NOT EXISTS seller_type VARCHAR(50);
ALTER TABLE seller_profiles ADD COLUMN IF NOT EXISTS display_name VARCHAR(100);
ALTER TABLE seller_profiles ADD COLUMN IF NOT EXISTS business_name VARCHAR(200);
ALTER TABLE seller_profiles ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE seller_profiles ADD COLUMN IF NOT EXISTS phone VARCHAR(20);
ALTER TABLE seller_profiles ADD COLUMN IF NOT EXISTS tax_id VARCHAR(50);
ALTER TABLE seller_profiles ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE seller_profiles ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';

-- Step 2: Backfill seller_type for existing profiles
-- Map legacy types: RETAIL_SELLER -> RETAILER, SHOP -> BUSINESS
UPDATE seller_profiles sp
SET seller_type = CASE 
    WHEN u.seller_type = 'FARMER' THEN 'FARMER'
    WHEN u.seller_type = 'WHOLESALER' THEN 'WHOLESALER'
    WHEN u.seller_type = 'RETAIL_SELLER' THEN 'RETAILER'
    WHEN u.seller_type = 'SHOP' THEN 'BUSINESS'
    ELSE 'INDIVIDUAL'
END
FROM users u
WHERE sp.user_id = u.id AND sp.seller_type IS NULL;

-- Step 3: Backfill email and display_name from users table
UPDATE seller_profiles sp
SET 
    email = COALESCE(sp.email, u.email),
    display_name = COALESCE(sp.display_name, u.username, CONCAT('Seller-', u.id))
FROM users u
WHERE sp.user_id = u.id AND (sp.email IS NULL OR sp.display_name IS NULL);

-- Step 4: Set default status for existing profiles
UPDATE seller_profiles
SET status = 'ACTIVE'
WHERE status IS NULL;

-- Step 5: Add NOT NULL constraints after backfilling
ALTER TABLE seller_profiles ALTER COLUMN seller_type SET NOT NULL;
ALTER TABLE seller_profiles ALTER COLUMN display_name SET NOT NULL;
ALTER TABLE seller_profiles ALTER COLUMN email SET NOT NULL;
ALTER TABLE seller_profiles ALTER COLUMN status SET NOT NULL;

-- Step 6: Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_seller_profiles_user_id ON seller_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_seller_profiles_status ON seller_profiles(status);
CREATE INDEX IF NOT EXISTS idx_seller_profiles_seller_type ON seller_profiles(seller_type);
CREATE INDEX IF NOT EXISTS idx_seller_profiles_email ON seller_profiles(email);

-- Step 7: Update users table seller_type enum constraint
-- Drop existing constraint if exists (PostgreSQL)
DO $$ 
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'users_seller_type_check'
    ) THEN
        ALTER TABLE users DROP CONSTRAINT users_seller_type_check;
    END IF;
END $$;

-- Add new constraint with updated values
ALTER TABLE users ADD CONSTRAINT users_seller_type_check 
CHECK (seller_type IN ('INDIVIDUAL', 'BUSINESS', 'FARMER', 'WHOLESALER', 'RETAILER'));

-- Step 8: Update existing seller_type values in users table
UPDATE users
SET seller_type = CASE 
    WHEN seller_type = 'RETAIL_SELLER' THEN 'RETAILER'
    WHEN seller_type = 'SHOP' THEN 'BUSINESS'
    ELSE seller_type
END
WHERE seller_type IN ('RETAIL_SELLER', 'SHOP');

-- Step 9: Add check constraint for seller_profiles.status
ALTER TABLE seller_profiles DROP CONSTRAINT IF EXISTS seller_profiles_status_check;
ALTER TABLE seller_profiles ADD CONSTRAINT seller_profiles_status_check 
CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'));

-- Step 10: Add check constraint for seller_profiles.seller_type
ALTER TABLE seller_profiles DROP CONSTRAINT IF EXISTS seller_profiles_seller_type_check;
ALTER TABLE seller_profiles ADD CONSTRAINT seller_profiles_seller_type_check 
CHECK (seller_type IN ('INDIVIDUAL', 'BUSINESS', 'FARMER', 'WHOLESALER', 'RETAILER'));

-- Step 11: Create unique constraint on user_id to ensure one profile per user
CREATE UNIQUE INDEX IF NOT EXISTS idx_seller_profiles_user_id_unique ON seller_profiles(user_id);

-- Migration Complete
-- Summary:
-- - Added unified seller architecture columns to seller_profiles
-- - Migrated legacy seller types (RETAIL_SELLER -> RETAILER, SHOP -> BUSINESS)
-- - Backfilled required fields from users table
-- - Added constraints and indexes for data integrity and performance
-- - Preserved legacy fields for backward compatibility
