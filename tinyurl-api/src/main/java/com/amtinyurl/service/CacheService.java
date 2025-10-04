package com.amtinyurl.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final String CODE_TO_URL_PREFIX = "code:";
    private static final String URL_TO_CODE_PREFIX = "url:";

    public void putCodeToUrl(String code, String url) {
        try {
            redisTemplate.opsForValue().set(CODE_TO_URL_PREFIX + code, url, TTL);
            log.debug("Cached code->url mapping: {} -> {}", code, url);
        } catch (Exception e) {
            log.warn("Failed to cache code->url mapping: {} -> {}", code, url, e);
        }
    }

    public void putUrlToCode(String url, String code) {
        try {
            redisTemplate.opsForValue().set(URL_TO_CODE_PREFIX + url, code, TTL);
            log.debug("Cached url->code mapping: {} -> {}", url, code);
        } catch (Exception e) {
            log.warn("Failed to cache url->code mapping: {} -> {}", url, code, e);
        }
    }

    public CacheResult getUrlByCode(String code) {
        try {
            String url = redisTemplate.opsForValue().get(CODE_TO_URL_PREFIX + code);
            if (url != null) {
                log.debug("Cache hit for code->url: {} -> {}", code, url);
                return new CacheResult(url, true);
            } else {
                log.debug("Cache miss for code->url: {}", code);
                return new CacheResult(null, false);
            }
        } catch (Exception e) {
            log.warn("Failed to get url by code from cache: {}", code, e);
            return new CacheResult(null, false);
        }
    }

    public CacheResult getCodeByUrl(String url) {
        try {
            String code = redisTemplate.opsForValue().get(URL_TO_CODE_PREFIX + url);
            if (code != null) {
                log.debug("Cache hit for url->code: {} -> {}", url, code);
                return new CacheResult(code, true);
            } else {
                log.debug("Cache miss for url->code: {}", url);
                return new CacheResult(null, false);
            }
        } catch (Exception e) {
            log.warn("Failed to get code by url from cache: {}", url, e);
            return new CacheResult(null, false);
        }
    }

    public void putBidirectional(String code, String url) {
        putCodeToUrl(code, url);
        putUrlToCode(url, code);
    }

    public static class CacheResult {
        public final String value;
        public final boolean hit;

        public CacheResult(String value, boolean hit) {
            this.value = value;
            this.hit = hit;
        }

        public String getCacheStatus() {
            return hit ? "hit" : "miss";
        }
    }
}