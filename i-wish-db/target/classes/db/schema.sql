-- ============================================================
-- i-Wish Database Schema
-- Run this ONCE to create the database and all tables.
-- Safe to re-run: DROP statements at the top reset everything
-- if you need a clean slate during development.
-- ============================================================

CREATE DATABASE IF NOT EXISTS iwish_db;
USE iwish_db;

-- Drop in reverse dependency order so foreign keys don't block us
DROP TABLE IF EXISTS Notifications;
DROP TABLE IF EXISTS Contributions;
DROP TABLE IF EXISTS WishItems;
DROP TABLE IF EXISTS CatalogItems;
DROP TABLE IF EXISTS Friends;
DROP TABLE IF EXISTS Users;

-- ---------------------------------------------------
-- Users
-- ---------------------------------------------------
CREATE TABLE Users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------
-- Friends (one row per relationship direction of the request)
-- status: 'pending' until the receiver accepts, then 'accepted'
-- ---------------------------------------------------
CREATE TABLE Friends (
    friend_id      INT AUTO_INCREMENT PRIMARY KEY,
    user_id        INT NOT NULL,               -- the person who SENT the request
    friend_user_id INT NOT NULL,                -- the person who RECEIVED the request
    status         ENUM('pending', 'accepted') NOT NULL DEFAULT 'pending',
    requested_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (friend_user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    UNIQUE KEY unique_pair (user_id, friend_user_id)
);

-- ---------------------------------------------------
-- CatalogItems (admin-seeded pool of purchasable items — spec #12)
-- ---------------------------------------------------
CREATE TABLE CatalogItems (
    item_id    INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    price      DECIMAL(10,2) NOT NULL,
    image_path VARCHAR(255)
);

-- ---------------------------------------------------
-- WishItems (a catalog item placed on a specific user's wish list)
-- ---------------------------------------------------
CREATE TABLE WishItems (
    wish_id       INT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT NOT NULL,
    item_id       INT NOT NULL,
    amount_raised DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    is_complete   BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES CatalogItems(item_id)
);

-- ---------------------------------------------------
-- Contributions (who paid how much toward which wish item)
-- ---------------------------------------------------
CREATE TABLE Contributions (
    contrib_id     INT AUTO_INCREMENT PRIMARY KEY,
    wish_id        INT NOT NULL,
    contributor_id INT NOT NULL,
    amount         DECIMAL(10,2) NOT NULL,
    contributed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (wish_id) REFERENCES WishItems(wish_id) ON DELETE CASCADE,
    FOREIGN KEY (contributor_id) REFERENCES Users(user_id)
);

-- ---------------------------------------------------
-- Notifications
-- type: 'buyer' = sent to a contributor when the item they funded completes
--       'receiver' = sent to the wish-list owner when their item completes
-- ---------------------------------------------------
CREATE TABLE Notifications (
    notif_id   INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    message    VARCHAR(255) NOT NULL,
    type       ENUM('buyer', 'receiver') NOT NULL,
    is_read    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
);
