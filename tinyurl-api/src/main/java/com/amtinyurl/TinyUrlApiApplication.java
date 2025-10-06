package com.amtinyurl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class TinyUrlApiApplication {

    private static final Logger logger = LoggerFactory.getLogger(TinyUrlApiApplication.class);

    @Autowired
    private Environment env;

    public static void main(String[] args) {
        SpringApplication.run(TinyUrlApiApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logEnvironmentVariables() {
        logger.info("🎯 ============================================");
        logger.info("🎯 FINAL RESOLVED CONFIGURATION (AFTER STARTUP)");
        logger.info("🎯 ============================================");
        logger.info("🔧 Spring Profiles Active: {}", String.join(",", env.getActiveProfiles()));
        logger.info("🎯 FINAL Datasource URL: {}", env.getProperty("spring.datasource.url"));
        logger.info("🔧 Datasource Username: {}", env.getProperty("spring.datasource.username"));
        logger.info("🔧 Redis URL: {}", env.getProperty("spring.data.redis.url"));
        logger.info("🔧 API Port: {}", env.getProperty("server.port"));
        logger.info("🔧 Base URL: {}", env.getProperty("app.base-url"));
        logger.info("🔧 JWT Secret: {}", env.getProperty("jwt.secret") != null ? "***SET***" : "NOT SET");

        // Log raw environment variables for debugging
        logger.info("📋 === Raw Environment Variables ===");
        logger.info("📋 MYSQL_URL env var: {}", System.getenv("MYSQL_URL"));
        logger.info("📋 MYSQL_USER env var: {}", System.getenv("MYSQL_USER"));
        logger.info("📋 REDIS_URL env var: {}", System.getenv("REDIS_URL"));
        logger.info("📋 SPRING_PROFILES_ACTIVE env var: {}", System.getenv("SPRING_PROFILES_ACTIVE"));

        // Critical validation
        String finalUrl = env.getProperty("spring.datasource.url");
        if (finalUrl != null && finalUrl.contains("tinyurl-mysql:3306")) {
            logger.info("✅ SUCCESS: Using correct container-to-container MySQL URL!");
        } else if (finalUrl != null && finalUrl.contains("localhost:3306")) {
            logger.error("❌ ERROR: Still using localhost:3306 - environment variables not working!");
        } else {
            logger.warn("⚠️ WARNING: Unexpected datasource URL: {}", finalUrl);
        }
        logger.info("🎯 ============================================");
    }
}