-- ============================================================
-- i-Wish Seed Data
-- Run this AFTER schema.sql.
-- Only seeds CatalogItems (the admin-provided pool of items).
-- Test USERS are deliberately NOT seeded here, because passwords
-- must go through the app's hashing logic (PasswordUtil), not be
-- inserted as plaintext. Run DBTestMain.java instead to create
-- test users the correct way — see instructions at the bottom
-- of this file.
-- ============================================================

USE iwish_db;

INSERT INTO CatalogItems (name, price, image_path) VALUES
('Headphones', 500.00, NULL),
('Smart Watch', 1200.00, NULL),
('Sneakers', 800.00, NULL),
('Backpack', 350.00, NULL),
('Bluetooth Speaker', 600.00, NULL),
('Sunglasses', 250.00, NULL),
('Perfume', 450.00, NULL),
('Gaming Mouse', 300.00, NULL);

-- To create test users (e.g. ahmed / mohamed / sara), run the
-- DBTestMain class in iwish.db — it calls UserDAO.createUser()
-- with a properly hashed password for each one and prints the
-- resulting user_id values.
