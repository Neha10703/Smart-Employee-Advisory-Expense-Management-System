-- Add payments table for admin monitoring
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    subscription_id BIGINT,
    razorpay_payment_id VARCHAR(255),
    razorpay_order_id VARCHAR(255),
    razorpay_signature VARCHAR(255),
    status ENUM('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED') NOT NULL DEFAULT 'PENDING',
    method ENUM('CARD', 'UPI', 'NETBANKING', 'WALLET'),
    amount DECIMAL(10, 2),
    currency VARCHAR(10) DEFAULT 'INR',
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_razorpay_payment_id (razorpay_payment_id),
    INDEX idx_status (status)
);

-- Insert sample payment data for testing
INSERT INTO payments (user_id, razorpay_payment_id, razorpay_order_id, status, method, amount, description) 
SELECT 
    u.id,
    CONCAT('pay_', SUBSTRING(MD5(RAND()), 1, 14)),
    CONCAT('order_', SUBSTRING(MD5(RAND()), 1, 14)),
    CASE 
        WHEN RAND() > 0.9 THEN 'FAILED'
        WHEN RAND() > 0.1 THEN 'SUCCESS'
        ELSE 'PENDING'
    END,
    CASE 
        WHEN RAND() > 0.7 THEN 'UPI'
        WHEN RAND() > 0.4 THEN 'CARD'
        WHEN RAND() > 0.2 THEN 'NETBANKING'
        ELSE 'WALLET'
    END,
    CASE 
        WHEN s.plan = 'GOLD' THEN 999.00
        WHEN s.plan = 'SILVER' THEN 499.00
        ELSE 0.00
    END,
    CONCAT('Payment for ', s.plan, ' subscription')
FROM users u
LEFT JOIN subscriptions s ON u.id = s.user_id
WHERE u.role = 'USER'
LIMIT 10;