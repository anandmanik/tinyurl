-- TinyURL Service Database Schema
-- Initial migration to create core tables

-- Table for storing unique URLs with their short codes
CREATE TABLE urls (
    code CHAR(7) NOT NULL PRIMARY KEY,
    normalized_url VARCHAR(2048) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_normalized_url (normalized_url)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

-- Table for associating users with their shortened URLs
CREATE TABLE user_urls (
    user_id_lower CHAR(6) NOT NULL,
    code CHAR(7) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id_lower, code),
    FOREIGN KEY (code) REFERENCES urls(code) ON DELETE CASCADE,
    INDEX idx_user_id_lower (user_id_lower)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;