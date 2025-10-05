-- Initialize TinyURL Database
-- This script runs when the MySQL container starts for the first time

USE tinyurl;

-- Create application user with proper permissions
CREATE USER IF NOT EXISTS 'tinyurl'@'%' IDENTIFIED BY 'tinyurl';
GRANT ALL PRIVILEGES ON tinyurl.* TO 'tinyurl'@'%';
FLUSH PRIVILEGES;

-- The actual tables will be created by Flyway migration when the backend starts