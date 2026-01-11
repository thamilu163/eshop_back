-- ============================================================================
-- Performance Optimization Indexes
-- Spring Boot E-Commerce Application
-- Version: 2.0.0
-- Description: Comprehensive indexing strategy for query optimization
-- ============================================================================

-- ============================================================================
-- PRODUCTS TABLE INDEXES
-- ============================================================================

-- Full-text search index for product name and description
CREATE INDEX IF NOT EXISTS idx_product_name_trgm 
    ON products USING gin(to_tsvector('english', name));

CREATE INDEX IF NOT EXISTS idx_product_description_trgm 
    ON products USING gin(to_tsvector('english', description));

CREATE INDEX IF NOT EXISTS idx_product_fulltext 
    ON products USING gin(to_tsvector('english', name || ' ' || COALESCE(description, '')));

-- Category filtering (most common query)
CREATE INDEX IF NOT EXISTS idx_product_category_id 
    ON products(category_id) 
    WHERE active = true;

-- Shop filtering
CREATE INDEX IF NOT EXISTS idx_product_shop_id 
    ON products(shop_id) 
    WHERE active = true;

-- Price range filtering
CREATE INDEX IF NOT EXISTS idx_product_price 
    ON products(price) 
    WHERE active = true;

-- Sorting by creation date (default sort)
CREATE INDEX IF NOT EXISTS idx_product_created_at 
    ON products(created_at DESC);

-- Composite index for common filter combinations
CREATE INDEX IF NOT EXISTS idx_product_active_category_created 
    ON products(active, category_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_product_filters 
    ON products(category_id, price, stock) 
    WHERE active = true;

-- Covering index for product list queries
CREATE INDEX IF NOT EXISTS idx_product_list_covering 
    ON products(id, name, price, stock, created_at) 
    INCLUDE (description) 
    WHERE active = true;

-- Partial indexes for specific queries
CREATE INDEX IF NOT EXISTS idx_product_out_of_stock 
    ON products(id, name) 
    WHERE stock = 0 AND active = true;

CREATE INDEX IF NOT EXISTS idx_product_low_stock 
    ON products(id, name, stock) 
    WHERE stock < 10 AND stock > 0 AND active = true;

-- SKU lookup (unique constraint already creates index)
CREATE UNIQUE INDEX IF NOT EXISTS idx_product_sku 
    ON products(sku) 
    WHERE sku IS NOT NULL;

-- Friendly URL lookup
CREATE INDEX IF NOT EXISTS idx_product_friendly_url 
    ON products(friendly_url);

-- ============================================================================
-- ORDERS TABLE INDEXES
-- ============================================================================

-- User's order history
CREATE INDEX IF NOT EXISTS idx_order_user_id 
    ON orders(user_id);

-- Order status filtering (for admin dashboard)
CREATE INDEX IF NOT EXISTS idx_order_status 
    ON orders(status) 
    WHERE status IN ('PENDING', 'PROCESSING', 'SHIPPED');

-- Recent orders (default sort)
CREATE INDEX IF NOT EXISTS idx_order_created_at 
    ON orders(created_at DESC);

-- Composite index for user orders with status filter
CREATE INDEX IF NOT EXISTS idx_order_user_status_created 
    ON orders(user_id, status, created_at DESC);

-- Shop orders
CREATE INDEX IF NOT EXISTS idx_order_shop_id 
    ON orders(shop_id);

-- Order number lookup
CREATE UNIQUE INDEX IF NOT EXISTS idx_order_number 
    ON orders(order_number);

-- ============================================================================
-- ORDER_ITEMS TABLE INDEXES
-- ============================================================================

-- Order items by order
CREATE INDEX IF NOT EXISTS idx_order_item_order_id 
    ON order_items(order_id);

-- Product sales analytics
CREATE INDEX IF NOT EXISTS idx_order_item_product_id 
    ON order_items(product_id);

-- Composite for order details fetch
CREATE INDEX IF NOT EXISTS idx_order_item_order_product 
    ON order_items(order_id, product_id);

-- ============================================================================
-- REVIEWS TABLE INDEXES
-- ============================================================================

-- Product reviews
CREATE INDEX IF NOT EXISTS idx_review_product_id 
    ON reviews(product_id, created_at DESC);

-- User's reviews
CREATE INDEX IF NOT EXISTS idx_review_user_id 
    ON reviews(user_id);

-- Rating filtering
CREATE INDEX IF NOT EXISTS idx_review_rating 
    ON reviews(rating);

-- Approved reviews only
CREATE INDEX IF NOT EXISTS idx_review_approved 
    ON reviews(product_id, created_at DESC) 
    WHERE approved = true;

-- ============================================================================
-- USERS TABLE INDEXES
-- ============================================================================

-- Email lookup (login)
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_email 
    ON users(email) 
    WHERE active = true;

-- Username lookup
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_username 
    ON users(username) 
    WHERE active = true;

-- Role filtering
CREATE INDEX IF NOT EXISTS idx_user_role 
    ON users(role);

-- ============================================================================
-- CATEGORIES TABLE INDEXES
-- ============================================================================

-- Parent-child relationship (category tree)
CREATE INDEX IF NOT EXISTS idx_category_parent_id 
    ON categories(parent_id);

-- Slug lookup
CREATE UNIQUE INDEX IF NOT EXISTS idx_category_slug 
    ON categories(slug);

-- Active categories
CREATE INDEX IF NOT EXISTS idx_category_active 
    ON categories(active, display_order);

-- ============================================================================
-- PAYMENTS TABLE INDEXES
-- ============================================================================

-- Order payment lookup
CREATE INDEX IF NOT EXISTS idx_payment_order_id 
    ON payments(order_id);

-- Transaction ID lookup (webhook verification)
CREATE INDEX IF NOT EXISTS idx_payment_transaction_id 
    ON payments(transaction_id);

-- Payment status and date (for reporting)
CREATE INDEX IF NOT EXISTS idx_payment_status_created 
    ON payments(status, created_at DESC);

-- Gateway filtering
CREATE INDEX IF NOT EXISTS idx_payment_gateway 
    ON payments(gateway, created_at DESC);

-- ============================================================================
-- SHOPS TABLE INDEXES
-- ============================================================================

-- Shop owner lookup
CREATE INDEX IF NOT EXISTS idx_shop_owner_id 
    ON shops(owner_id);

-- Active shops
CREATE INDEX IF NOT EXISTS idx_shop_active 
    ON shops(active, created_at DESC);

-- Slug lookup
CREATE UNIQUE INDEX IF NOT EXISTS idx_shop_slug 
    ON shops(slug);

-- ============================================================================
-- COUPONS TABLE INDEXES
-- ============================================================================

-- Coupon code lookup
CREATE UNIQUE INDEX IF NOT EXISTS idx_coupon_code 
    ON coupons(code);

-- Active coupons with validity
CREATE INDEX IF NOT EXISTS idx_coupon_active_valid 
    ON coupons(active, valid_from, valid_until) 
    WHERE active = true;

-- ============================================================================
-- WISHLIST TABLE INDEXES
-- ============================================================================

-- User's wishlist
CREATE INDEX IF NOT EXISTS idx_wishlist_user_id 
    ON wishlist(user_id);

-- Product in wishlists (for analytics)
CREATE INDEX IF NOT EXISTS idx_wishlist_product_id 
    ON wishlist(product_id);

-- Composite for wishlist check
CREATE UNIQUE INDEX IF NOT EXISTS idx_wishlist_user_product 
    ON wishlist(user_id, product_id);

-- ============================================================================
-- CART TABLE INDEXES
-- ============================================================================

-- User's cart
CREATE INDEX IF NOT EXISTS idx_cart_user_id 
    ON cart(user_id);

-- Product in carts
CREATE INDEX IF NOT EXISTS idx_cart_product_id 
    ON cart(product_id);

-- Session cart (guest users)
CREATE INDEX IF NOT EXISTS idx_cart_session_id 
    ON cart(session_id) 
    WHERE session_id IS NOT NULL;

-- ============================================================================
-- AUDIT_LOGS TABLE INDEXES
-- ============================================================================

-- Entity audit trail
CREATE INDEX IF NOT EXISTS idx_audit_entity_type_id 
    ON audit_logs(entity_type, entity_id, created_at DESC);

-- User activity
CREATE INDEX IF NOT EXISTS idx_audit_user_id 
    ON audit_logs(user_id, created_at DESC);

-- Action filtering
CREATE INDEX IF NOT EXISTS idx_audit_action 
    ON audit_logs(action, created_at DESC);

-- Recent audits
CREATE INDEX IF NOT EXISTS idx_audit_created_at 
    ON audit_logs(created_at DESC);

-- ============================================================================
-- UPDATE TABLE STATISTICS
-- ============================================================================

ANALYZE products;
ANALYZE orders;
ANALYZE order_items;
ANALYZE reviews;
ANALYZE users;
ANALYZE categories;
ANALYZE payments;
ANALYZE shops;
ANALYZE coupons;
ANALYZE wishlist;
ANALYZE cart;
ANALYZE audit_logs;

-- ============================================================================
-- PERFORMANCE NOTES
-- ============================================================================

-- 1. All indexes are created with IF NOT EXISTS to allow safe re-execution
-- 2. Partial indexes reduce index size and improve performance for filtered queries
-- 3. Covering indexes reduce table lookups for list queries
-- 4. Full-text indexes enable fast search without LIKE %query%
-- 5. Composite indexes support multi-column WHERE and ORDER BY clauses
-- 6. ANALYZE updates table statistics for query planner optimization
-- 7. Regular VACUUM and ANALYZE should be scheduled in production
-- 8. Monitor index usage with pg_stat_user_indexes and drop unused ones

-- ============================================================================
-- MONITORING QUERIES
-- ============================================================================

-- View index usage statistics:
-- SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
-- FROM pg_stat_user_indexes
-- WHERE schemaname = 'public'
-- ORDER BY idx_scan DESC;

-- Find unused indexes:
-- SELECT schemaname, tablename, indexname
-- FROM pg_stat_user_indexes
-- WHERE idx_scan = 0 AND schemaname = 'public';

-- View table sizes:
-- SELECT tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
-- FROM pg_tables
-- WHERE schemaname = 'public'
-- ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
