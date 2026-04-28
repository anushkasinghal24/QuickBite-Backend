-- ============================================================
-- QuickBite — payment-service Database Schema
-- Database: quickbite_payment
-- JPA ddl-auto=update auto-creates. This file is REFERENCE only.
-- ============================================================

CREATE DATABASE IF NOT EXISTS quickbite_payment;
USE quickbite_payment;

-- ===== PAYMENTS =====
CREATE TABLE IF NOT EXISTS payments (
    payment_id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id              BIGINT       NOT NULL,
    customer_id           BIGINT       NOT NULL,
    amount                DOUBLE       NOT NULL,
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    mode                  VARCHAR(20)  NOT NULL,
    transaction_id        VARCHAR(100) UNIQUE,
    currency              VARCHAR(10)  DEFAULT 'INR',
    refund_transaction_id VARCHAR(100),
    failure_reason        VARCHAR(300),
    created_at            DATETIME     NOT NULL,
    paid_at               DATETIME,
    refunded_at           DATETIME,

    INDEX idx_order_id    (order_id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_status      (status)
);

-- ===== WALLETS =====
CREATE TABLE IF NOT EXISTS wallets (
    wallet_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT       NOT NULL UNIQUE,
    balance     DOUBLE       NOT NULL DEFAULT 0.0,
    currency    VARCHAR(10)  DEFAULT 'INR',
    created_at  DATETIME,
    updated_at  DATETIME,

    INDEX idx_wallet_customer_id (customer_id)
);

-- ===== WALLET_STATEMENTS =====
CREATE TABLE IF NOT EXISTS wallet_statements (
    statement_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_id       BIGINT       NOT NULL,
    customer_id     BIGINT       NOT NULL,
    amount          DOUBLE       NOT NULL,
    type            VARCHAR(10)  NOT NULL,   -- CREDIT / DEBIT
    description     VARCHAR(300),
    closing_balance DOUBLE       NOT NULL,
    reference_id    VARCHAR(100),
    created_at      DATETIME     NOT NULL,

    FOREIGN KEY (wallet_id) REFERENCES wallets(wallet_id),
    INDEX idx_stmt_wallet_id   (wallet_id),
    INDEX idx_stmt_customer_id (customer_id),
    INDEX idx_stmt_type        (type)
);

-- ===== SAMPLE DATA (for testing) =====
INSERT INTO wallets (customer_id, balance, currency, created_at, updated_at) VALUES
(1, 500.0,  'INR', NOW(), NOW()),
(2, 1000.0, 'INR', NOW(), NOW());

INSERT INTO wallet_statements (wallet_id, customer_id, amount, type, description, closing_balance, reference_id, created_at) VALUES
(1, 1, 500.0,  'CREDIT', 'Wallet top-up via CARD', 500.0,  'QB-TXN-INIT001', NOW()),
(2, 2, 1000.0, 'CREDIT', 'Wallet top-up via UPI',  1000.0, 'QB-TXN-INIT002', NOW());

INSERT INTO payments (order_id, customer_id, amount, status, mode, transaction_id, currency, created_at, paid_at) VALUES
(101, 1, 350.0, 'PAID',     'WALLET', 'QB-TXN-A1B2C3D4', 'INR', NOW(), NOW()),
(102, 2, 480.0, 'PAID',     'COD',    'QB-TXN-E5F6G7H8', 'INR', NOW(), NOW()),
(103, 1, 220.0, 'REFUNDED', 'WALLET', 'QB-TXN-I9J0K1L2', 'INR', NOW(), NOW());

-- ===== NOTIFICATIONS =====
CREATE TABLE IF NOT EXISTS notifications (
    notification_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id     BIGINT       NOT NULL,
    type             VARCHAR(30)  NOT NULL,
    title            VARCHAR(200) NOT NULL,
    message          VARCHAR(500) NOT NULL,
    channel          VARCHAR(10)  NOT NULL DEFAULT 'APP',
    related_id       BIGINT,
    related_type     VARCHAR(20),
    deep_link_url    VARCHAR(300),
    is_read          BOOLEAN      DEFAULT FALSE,
    sent_at          DATETIME     NOT NULL,
    read_at          DATETIME,

    INDEX idx_recipient_id   (recipient_id),
    INDEX idx_is_read        (is_read),
    INDEX idx_type           (type),
    INDEX idx_related_id     (related_id),
    INDEX idx_recipient_read (recipient_id, is_read)
);

INSERT INTO notifications (recipient_id, type, title, message, channel, related_id, related_type, deep_link_url, is_read, sent_at) VALUES
(1, 'ORDER_PLACED',    'Order Placed!',       'Your order #101 has been placed successfully.',         'APP', 101, 'ORDER', '/orders/101', FALSE, NOW()),
(1, 'ORDER_CONFIRMED', 'Order Confirmed',     'Your order #101 has been confirmed by the restaurant.', 'APP', 101, 'ORDER', '/orders/101', FALSE, NOW()),
(1, 'PAYMENT_RECEIPT', 'Payment Successful',  'Payment of ₹350 received for order #101.',               'APP', 201, 'PAYMENT', '/wallet',   TRUE,  NOW()),
(2, 'NEW_ORDER_ALERT', 'New Order!',          'New order #101 received! Check your dashboard.',        'APP', 101, 'ORDER', '/dashboard', FALSE, NOW()),
(2, 'RESTAURANT_APPROVED', 'Restaurant Approved!', 'Your restaurant is now live on QuickBite!',          'APP', 1,   'RESTAURANT', '/dashboard', TRUE, NOW());
