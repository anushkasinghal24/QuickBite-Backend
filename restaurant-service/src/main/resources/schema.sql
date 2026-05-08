-- ============================================================
-- QuickBite — restaurant-service Database Schema
-- Database: quickbite_restaurant
-- JPA ddl-auto=update will auto-create these tables.
-- This file is for REFERENCE and manual setup if needed.
-- ============================================================

CREATE DATABASE IF NOT EXISTS quickbite_restaurant;
USE quickbite_restaurant;

-- ===== RESTAURANTS TABLE =====
CREATE TABLE IF NOT EXISTS restaurants (
    restaurant_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id              BIGINT        NOT NULL,          -- userId from auth-service
    name                  VARCHAR(150)  NOT NULL,
    description           VARCHAR(500),
    cuisine               VARCHAR(100)  NOT NULL,
    address               VARCHAR(300)  NOT NULL,
    city                  VARCHAR(100)  NOT NULL,
    state                 VARCHAR(100),
    pincode               VARCHAR(10),
    latitude              DOUBLE        NOT NULL,
    longitude             DOUBLE        NOT NULL,
    phone                 VARCHAR(15)   NOT NULL,
    email                 VARCHAR(100),
    image_url             VARCHAR(500),
    avg_rating            DOUBLE        DEFAULT 0.0,
    is_open               BOOLEAN       DEFAULT FALSE,
    approval_status       VARCHAR(20)   DEFAULT 'PENDING',  -- PENDING/APPROVED/REJECTED
    rejection_reason      VARCHAR(300),
    delivery_radius       DOUBLE        DEFAULT 5.0,
    min_order_amount      DOUBLE        DEFAULT 0.0,
    estimated_delivery_min INT          DEFAULT 30,
    opening_time          VARCHAR(10),
    closing_time          VARCHAR(10),
    total_reviews         INT           DEFAULT 0,
    is_active             BOOLEAN       DEFAULT TRUE,
    created_at            DATETIME      NOT NULL,
    updated_at            DATETIME,

    INDEX idx_owner_id    (owner_id),
    INDEX idx_city        (city),
    INDEX idx_cuisine     (cuisine),
    INDEX idx_is_approved (approval_status),
    INDEX idx_lat_lng     (latitude, longitude)
);

-- ===== MENU CATEGORIES TABLE =====
CREATE TABLE IF NOT EXISTS menu_categories (
    category_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id   BIGINT        NOT NULL,
    name            VARCHAR(100)  NOT NULL,
    description     VARCHAR(300),
    image_url       VARCHAR(500),
    display_order   INT           DEFAULT 1,
    is_active       BOOLEAN       DEFAULT TRUE,

    FOREIGN KEY (restaurant_id) REFERENCES restaurants(restaurant_id) ON DELETE CASCADE,
    INDEX idx_cat_restaurant_id (restaurant_id),
    INDEX idx_display_order     (display_order)
);

-- ===== MENU ITEMS TABLE =====
CREATE TABLE IF NOT EXISTS menu_items (
    item_id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id     BIGINT        NOT NULL,
    category_id       BIGINT        NOT NULL,
    name              VARCHAR(150)  NOT NULL,
    description       VARCHAR(400),
    price             DOUBLE        NOT NULL,
    discounted_price  DOUBLE,
    image_url         VARCHAR(500),
    is_veg            BOOLEAN       DEFAULT TRUE,
    is_available      BOOLEAN       DEFAULT TRUE,
    rating            DOUBLE        DEFAULT 0.0,
    calories          INT,
    tags              VARCHAR(200),
    is_active         BOOLEAN       DEFAULT TRUE,
    created_at        DATETIME,
    updated_at        DATETIME,

    FOREIGN KEY (category_id) REFERENCES menu_categories(category_id) ON DELETE CASCADE,
    INDEX idx_item_restaurant_id (restaurant_id),
    INDEX idx_item_category_id   (category_id),
    INDEX idx_is_veg             (is_veg),
    INDEX idx_is_available       (is_available)
);

-- ===== SAMPLE DATA =====
INSERT INTO restaurants (owner_id, name, description, cuisine, address, city, latitude, longitude, phone, avg_rating, is_open, approval_status, delivery_radius, min_order_amount, estimated_delivery_min, is_active, created_at)
VALUES
(1, 'Punjabi Dhaba', 'Authentic North Indian food', 'North Indian', '12 Connaught Place', 'Delhi', 28.6315, 77.2167, '9811111111', 4.5, TRUE, 'APPROVED', 8.0, 100.0, 25, TRUE, NOW()),
(2, 'South Spice', 'Best South Indian in town', 'South Indian', '45 MG Road', 'Bangalore', 12.9716, 77.5946, '9822222222', 4.2, TRUE, 'APPROVED', 6.0, 150.0, 30, TRUE, NOW());

INSERT INTO menu_categories (restaurant_id, name, description, display_order, is_active)
VALUES
(1, 'Starters', 'Delicious appetizers', 1, TRUE),
(1, 'Main Course', 'Hearty mains', 2, TRUE),
(1, 'Breads', 'Fresh breads from tandoor', 3, TRUE),
(2, 'Dosa', 'Crispy dosas', 1, TRUE),
(2, 'Rice', 'Flavoured rice dishes', 2, TRUE);

INSERT INTO menu_items (restaurant_id, category_id, name, price, discounted_price, is_veg, is_available, is_active, created_at)
VALUES
(1, 1, 'Paneer Tikka',      280.0, 250.0, TRUE,  TRUE, TRUE, NOW()),
(1, 1, 'Chicken Tikka',     320.0, NULL,  FALSE, TRUE, TRUE, NOW()),
(1, 2, 'Dal Makhani',       220.0, 200.0, TRUE,  TRUE, TRUE, NOW()),
(1, 2, 'Butter Chicken',    350.0, NULL,  FALSE, TRUE, TRUE, NOW()),
(1, 3, 'Butter Naan',        50.0, NULL,  TRUE,  TRUE, TRUE, NOW()),
(2, 4, 'Masala Dosa',       120.0, 100.0, TRUE,  TRUE, TRUE, NOW()),
(2, 4, 'Rava Dosa',         130.0, NULL,  TRUE,  TRUE, TRUE, NOW()),
(2, 5, 'Veg Biryani',       200.0, 180.0, TRUE,  TRUE, TRUE, NOW());
