-- V100__Add_Performance_Indexes.sql
-- Performance optimization indexes for analytics and dashboard queries
-- Author: EShop Team
-- Date: 2025-12-14
-- Version: 2.0

-- ========================================
-- PRODUCTS TABLE INDEXES
-- ========================================

-- High-frequency JOIN columns
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_category_id 
    ON products(category_id) 
    WHERE deleted = false;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_brand_id 
    ON products(brand_id) 
    WHERE deleted = false;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_shop_id 
    ON products(shop_id) 
    WHERE deleted = false;

-- Query filters for product listing
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_active_featured 
    ON products(active, featured) 
    WHERE deleted = false;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_sku 
    ON products(sku) 
    WHERE deleted = false;

-- Partial index for active products (most common query)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_active_created 
    ON products(created_at DESC) 
    WHERE deleted = false AND active = true;

-- Composite index for product listing with category filter
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_list_optimized 
    ON products(category_id, active, created_at DESC) 
    WHERE deleted = false;

-- Search optimization (requires pg_trgm extension)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_name_trgm 
    ON products USING gin(name gin_trgm_ops);

-- Full-text search index
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_search_vector 
    ON products USING gin(
        to_tsvector('english', name || ' ' || COALESCE(description, ''))
    );

-- ========================================
-- ORDERS TABLE INDEXES
-- ========================================

-- Customer order history (very frequent query)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_customer_created 
    ON orders(customer_id, created_at DESC);

-- Order status filtering
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_status_created 
    ON orders(status, created_at DESC);

-- Seller order management
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_shop_created 
    ON orders(shop_id, created_at DESC) 
    WHERE status NOT IN ('CANCELLED', 'REFUNDED');

-- Delivery agent assignments
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_delivery_agent_status 
    ON orders(delivery_agent_id, status) 
    WHERE delivery_agent_id IS NOT NULL;

-- Analytics queries (completed orders)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_completed_date 
    ON orders(created_at) 
    WHERE status = 'COMPLETED';

-- ========================================
-- ORDER_ITEMS TABLE INDEXES
-- ========================================

-- Order item lookups
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_order_items_order_id 
    ON order_items(order_id);

-- Product sales analytics
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_order_items_product_id 
    ON order_items(product_id);

-- ========================================
-- INVENTORY TABLE INDEXES
-- ========================================

-- Product inventory lookups
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_inventory_product_shop 
    ON inventory(product_id, shop_id);

-- Low stock alerts
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_inventory_low_stock 
    ON inventory(shop_id, available_quantity) 
    WHERE available_quantity < 10;

-- ========================================
-- USERS TABLE INDEXES
-- ========================================

-- Email lookups (case-insensitive)
CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS idx_users_email_lower 
    ON users(LOWER(email));

-- Username lookups (case-insensitive)
CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS idx_users_username_lower 
    ON users(LOWER(username));

-- Role-based queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_role 
    ON users(role) 
    WHERE deleted = false;

-- Active users filter
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_active 
    ON users(active, created_at DESC) 
    WHERE deleted = false;

-- ========================================
-- PAYMENTS TABLE INDEXES
-- ========================================

-- Payment status queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payments_created_status 
    ON payments(created_at, status);

-- Order payment lookups
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payments_order_id 
    ON payments(order_id);

-- ========================================
-- PRODUCT_REVIEWS TABLE INDEXES
-- ========================================

-- Product reviews lookup
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_product_reviews_product_id 
    ON product_reviews(product_id, created_at DESC) 
    WHERE approved = true;

-- Customer reviews
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_product_reviews_customer_id 
    ON product_reviews(customer_id);

-- ========================================
-- SHOPS TABLE INDEXES
-- ========================================

-- Seller shop lookups
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_shops_seller_id 
    ON shops(seller_id) 
    WHERE deleted = false;

-- Active shops
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_shops_active 
    ON shops(active) 
    WHERE deleted = false;

-- ========================================
-- NOTIFICATIONS TABLE INDEXES
-- ========================================

-- User notifications (if table exists)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_notifications_user_created 
    ON notifications(user_id, created_at DESC) 
    WHERE read = false;

-- ========================================
-- ANALYZE TABLES FOR QUERY PLANNER
-- ========================================

ANALYZE products;
ANALYZE orders;
ANALYZE order_items;
ANALYZE inventory;
ANALYZE users;
ANALYZE payments;
ANALYZE product_reviews;
ANALYZE shops;
