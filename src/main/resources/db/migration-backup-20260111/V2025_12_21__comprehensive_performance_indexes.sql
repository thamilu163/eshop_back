-- ===========================================================================
-- Migration: V2025_12_21__comprehensive_performance_indexes.sql
-- Description: Add comprehensive database indexes for performance optimization
-- Author: System
-- Date: 2025-12-21
-- Target: Week 3-4 Performance Optimization
-- ===========================================================================

-- ===========================================================================
-- PRODUCTS TABLE INDEXES
-- ===========================================================================

-- Foreign key indexes (critical for JOIN performance)
CREATE INDEX IF NOT EXISTS idx_products_category_id 
    ON products(category_id) 
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_products_brand_id 
    ON products(brand_id) 
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_products_shop_id 
    ON products(shop_id) 
    WHERE deleted_at IS NULL;

-- Search and filter indexes
CREATE INDEX IF NOT EXISTS idx_products_name 
    ON products(name) 
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_products_sku 
    ON products(sku) 
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_products_url_slug 
    ON products(url_slug) 
    WHERE deleted_at IS NULL AND url_slug IS NOT NULL;

-- Price range queries
CREATE INDEX IF NOT EXISTS idx_products_price 
    ON products(price) 
    WHERE deleted_at IS NULL AND active = true;

CREATE INDEX IF NOT EXISTS idx_products_discount_price 
    ON products(discount_price) 
    WHERE deleted_at IS NULL AND discount_price IS NOT NULL;

-- Stock and availability
CREATE INDEX IF NOT EXISTS idx_products_stock_quantity 
    ON products(stock_quantity) 
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_products_active 
    ON products(active) 
    WHERE deleted_at IS NULL;

-- Featured products (homepage)
CREATE INDEX IF NOT EXISTS idx_products_featured 
    ON products(featured, created_at DESC) 
    WHERE deleted_at IS NULL AND active = true AND featured = true;

-- Composite index for product listing with filters
CREATE INDEX IF NOT EXISTS idx_products_listing 
    ON products(active, category_id, brand_id, created_at DESC) 
    WHERE deleted_at IS NULL;

-- Average rating for sorting
CREATE INDEX IF NOT EXISTS idx_products_average_rating 
    ON products(average_rating DESC NULLS LAST) 
    WHERE deleted_at IS NULL AND active = true;

-- ===========================================================================
-- ORDERS TABLE INDEXES
-- ===========================================================================

-- Foreign key indexes
CREATE INDEX IF NOT EXISTS idx_orders_user_id 
    ON orders(user_id);

CREATE INDEX IF NOT EXISTS idx_orders_shop_id 
    ON orders(shop_id);

-- Order status for admin/seller dashboards
CREATE INDEX IF NOT EXISTS idx_orders_status 
    ON orders(status, created_at DESC);

-- User order history
CREATE INDEX IF NOT EXISTS idx_orders_user_created 
    ON orders(user_id, created_at DESC);

-- Shop orders
CREATE INDEX IF NOT EXISTS idx_orders_shop_created 
    ON orders(shop_id, created_at DESC);

-- Payment status tracking
CREATE INDEX IF NOT EXISTS idx_orders_payment_status 
    ON orders(payment_status, created_at DESC);

-- Order tracking number lookup
CREATE INDEX IF NOT EXISTS idx_orders_tracking_number 
    ON orders(tracking_number) 
    WHERE tracking_number IS NOT NULL;

-- ===========================================================================
-- ORDER_ITEMS TABLE INDEXES
-- ===========================================================================

-- Foreign key indexes (prevent N+1 queries)
CREATE INDEX IF NOT EXISTS idx_order_items_order_id 
    ON order_items(order_id);

CREATE INDEX IF NOT EXISTS idx_order_items_product_id 
    ON order_items(product_id);

-- Composite for order details with product info
CREATE INDEX IF NOT EXISTS idx_order_items_order_product 
    ON order_items(order_id, product_id);

-- ===========================================================================
-- USERS TABLE INDEXES
-- ===========================================================================

-- Email lookup (login)
CREATE INDEX IF NOT EXISTS idx_users_email 
    ON users(email) 
    WHERE deleted_at IS NULL;

-- Phone lookup
CREATE INDEX IF NOT EXISTS idx_users_phone 
    ON users(phone) 
    WHERE deleted_at IS NULL AND phone IS NOT NULL;

-- Role-based queries
CREATE INDEX IF NOT EXISTS idx_users_role 
    ON users(role) 
    WHERE deleted_at IS NULL;

-- Active users
CREATE INDEX IF NOT EXISTS idx_users_active 
    ON users(active) 
    WHERE deleted_at IS NULL;

-- Email verification status
CREATE INDEX IF NOT EXISTS idx_users_email_verified 
    ON users(email_verified, created_at DESC) 
    WHERE deleted_at IS NULL;

-- ===========================================================================
-- CART TABLE INDEXES
-- ===========================================================================

-- User cart lookup
CREATE INDEX IF NOT EXISTS idx_cart_user_id 
    ON cart(user_id);

-- ===========================================================================
-- CART_ITEMS TABLE INDEXES
-- ===========================================================================

-- Foreign key indexes
CREATE INDEX IF NOT EXISTS idx_cart_items_cart_id 
    ON cart_items(cart_id);

CREATE INDEX IF NOT EXISTS idx_cart_items_product_id 
    ON cart_items(product_id);

-- Composite for cart details
CREATE INDEX IF NOT EXISTS idx_cart_items_cart_product 
    ON cart_items(cart_id, product_id);

-- ===========================================================================
-- WISHLIST TABLE INDEXES
-- ===========================================================================

-- User wishlist lookup
CREATE INDEX IF NOT EXISTS idx_wishlist_user_id 
    ON wishlist(user_id);

-- ===========================================================================
-- WISHLIST_ITEMS TABLE INDEXES
-- ===========================================================================

-- Foreign key indexes
CREATE INDEX IF NOT EXISTS idx_wishlist_items_wishlist_id 
    ON wishlist_items(wishlist_id);

CREATE INDEX IF NOT EXISTS idx_wishlist_items_product_id 
    ON wishlist_items(product_id);

-- Composite index
CREATE INDEX IF NOT EXISTS idx_wishlist_items_wishlist_product 
    ON wishlist_items(wishlist_id, product_id);

-- ===========================================================================
-- REVIEWS TABLE INDEXES
-- ===========================================================================

-- Product reviews
CREATE INDEX IF NOT EXISTS idx_reviews_product_id 
    ON reviews(product_id, created_at DESC);

-- User reviews
CREATE INDEX IF NOT EXISTS idx_reviews_user_id 
    ON reviews(user_id, created_at DESC);

-- Rating filter
CREATE INDEX IF NOT EXISTS idx_reviews_rating 
    ON reviews(rating, created_at DESC);

-- Verified purchase reviews
CREATE INDEX IF NOT EXISTS idx_reviews_verified 
    ON reviews(verified_purchase, rating DESC) 
    WHERE verified_purchase = true;

-- ===========================================================================
-- PAYMENTS TABLE INDEXES
-- ===========================================================================

-- Order payments
CREATE INDEX IF NOT EXISTS idx_payments_order_id 
    ON payments(order_id);

-- Payment status
CREATE INDEX IF NOT EXISTS idx_payments_status 
    ON payments(status, created_at DESC);

-- Transaction ID lookup
CREATE INDEX IF NOT EXISTS idx_payments_transaction_id 
    ON payments(transaction_id) 
    WHERE transaction_id IS NOT NULL;

-- Payment method analytics
CREATE INDEX IF NOT EXISTS idx_payments_method 
    ON payments(payment_method, created_at DESC);

-- ===========================================================================
-- SHOPS TABLE INDEXES
-- ===========================================================================

-- Owner lookup
CREATE INDEX IF NOT EXISTS idx_shops_owner_id 
    ON shops(owner_id);

-- Active shops
CREATE INDEX IF NOT EXISTS idx_shops_active 
    ON shops(active, created_at DESC) 
    WHERE deleted_at IS NULL;

-- Shop name search
CREATE INDEX IF NOT EXISTS idx_shops_name 
    ON shops(name) 
    WHERE deleted_at IS NULL;

-- ===========================================================================
-- CATEGORIES TABLE INDEXES
-- ===========================================================================

-- Parent category (hierarchical queries)
CREATE INDEX IF NOT EXISTS idx_categories_parent_id 
    ON categories(parent_id) 
    WHERE deleted_at IS NULL;

-- Category name lookup
CREATE INDEX IF NOT EXISTS idx_categories_name 
    ON categories(name) 
    WHERE deleted_at IS NULL;

-- URL slug lookup
CREATE INDEX IF NOT EXISTS idx_categories_url_slug 
    ON categories(url_slug) 
    WHERE deleted_at IS NULL;

-- Active categories
CREATE INDEX IF NOT EXISTS idx_categories_active 
    ON categories(active) 
    WHERE deleted_at IS NULL AND active = true;

-- ===========================================================================
-- BRANDS TABLE INDEXES
-- ===========================================================================

-- Brand name search
CREATE INDEX IF NOT EXISTS idx_brands_name 
    ON brands(name) 
    WHERE deleted_at IS NULL;

-- URL slug lookup
CREATE INDEX IF NOT EXISTS idx_brands_url_slug 
    ON brands(url_slug) 
    WHERE deleted_at IS NULL;

-- ===========================================================================
-- COUPONS TABLE INDEXES
-- ===========================================================================

-- Coupon code lookup (apply coupon)
CREATE INDEX IF NOT EXISTS idx_coupons_code 
    ON coupons(code) 
    WHERE deleted_at IS NULL AND active = true;

-- Valid coupons
CREATE INDEX IF NOT EXISTS idx_coupons_valid 
    ON coupons(active, start_date, end_date) 
    WHERE deleted_at IS NULL AND active = true;

-- ===========================================================================
-- PRODUCT_IMAGES TABLE INDEXES
-- ===========================================================================

-- Product images lookup
CREATE INDEX IF NOT EXISTS idx_product_images_product_id 
    ON product_images(product_id, display_order);

-- ===========================================================================
-- ADDRESSES TABLE INDEXES
-- ===========================================================================

-- User addresses
CREATE INDEX IF NOT EXISTS idx_addresses_user_id 
    ON addresses(user_id);

-- Default address lookup
CREATE INDEX IF NOT EXISTS idx_addresses_default 
    ON addresses(user_id, is_default) 
    WHERE is_default = true;

-- ===========================================================================
-- AUDIT_LOG TABLE INDEXES (if exists)
-- ===========================================================================

CREATE INDEX IF NOT EXISTS idx_audit_log_entity 
    ON audit_log(entity_type, entity_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_log_user 
    ON audit_log(user_identifier, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_log_action 
    ON audit_log(action, created_at DESC);

-- ===========================================================================
-- COMPOSITE INDEXES FOR COMMON QUERIES
-- ===========================================================================

-- Product search with filters (high traffic)
CREATE INDEX IF NOT EXISTS idx_products_search_filters 
    ON products(active, category_id, price, average_rating DESC, created_at DESC) 
    WHERE deleted_at IS NULL AND active = true;

-- Order dashboard queries
CREATE INDEX IF NOT EXISTS idx_orders_dashboard 
    ON orders(status, payment_status, created_at DESC);

-- User activity tracking
CREATE INDEX IF NOT EXISTS idx_orders_user_status 
    ON orders(user_id, status, created_at DESC);

-- ===========================================================================
-- PERFORMANCE STATISTICS UPDATE
-- ===========================================================================

-- Analyze all tables to update query planner statistics
ANALYZE products;
ANALYZE orders;
ANALYZE order_items;
ANALYZE users;
ANALYZE cart;
ANALYZE cart_items;
ANALYZE wishlist;
ANALYZE wishlist_items;
ANALYZE reviews;
ANALYZE payments;
ANALYZE shops;
ANALYZE categories;
ANALYZE brands;
ANALYZE coupons;
ANALYZE product_images;
ANALYZE addresses;

-- ===========================================================================
-- INDEX USAGE MONITORING QUERY
-- ===========================================================================

-- Run this query to monitor index usage:
-- SELECT
--     schemaname,
--     tablename,
--     indexname,
--     idx_scan as index_scans,
--     idx_tup_read as tuples_read,
--     idx_tup_fetch as tuples_fetched
-- FROM pg_stat_user_indexes
-- WHERE schemaname = 'public'
-- ORDER BY idx_scan DESC;

-- ===========================================================================
-- QUERY PERFORMANCE MONITORING
-- ===========================================================================

-- Enable pg_stat_statements for query performance monitoring (run once):
-- CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Query to find slow queries:
-- SELECT
--     query,
--     calls,
--     total_exec_time,
--     mean_exec_time,
--     max_exec_time
-- FROM pg_stat_statements
-- WHERE mean_exec_time > 100  -- queries taking more than 100ms
-- ORDER BY mean_exec_time DESC
-- LIMIT 20;

-- ===========================================================================
-- END OF MIGRATION
-- ===========================================================================
