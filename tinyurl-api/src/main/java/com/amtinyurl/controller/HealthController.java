package com.amtinyurl.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private final DataSource dataSource;
    private final RedisTemplate<String, String> redisTemplate;

    @GetMapping("/healthz")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        boolean allHealthy = true;

        health.put("status", "ok");
        health.put("checks", new HashMap<String, String>());
        Map<String, String> checks = (Map<String, String>) health.get("checks");

        try {
            try (Connection connection = dataSource.getConnection()) {
                if (connection.isValid(5)) {
                    checks.put("mysql", "ok");
                } else {
                    checks.put("mysql", "failed");
                    allHealthy = false;
                }
            }
        } catch (Exception e) {
            log.warn("MySQL health check failed", e);
            checks.put("mysql", "failed");
            allHealthy = false;
        }

        try {
            redisTemplate.opsForValue().set("health:check", "ok");
            String result = redisTemplate.opsForValue().get("health:check");
            if ("ok".equals(result)) {
                checks.put("redis", "ok");
            } else {
                checks.put("redis", "failed");
                allHealthy = false;
            }
        } catch (Exception e) {
            log.warn("Redis health check failed", e);
            checks.put("redis", "failed");
            allHealthy = false;
        }

        if (!allHealthy) {
            health.put("status", "failed");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }

        return ResponseEntity.ok(health);
    }

    @GetMapping("/api/healthz")
    public ResponseEntity<Map<String, Object>> apiHealth() {
        return health();
    }
}