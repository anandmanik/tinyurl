package com.amtinyurl.service;

import com.amtinyurl.entity.Url;
import com.amtinyurl.entity.UserUrl;
import com.amtinyurl.repository.UrlRepository;
import com.amtinyurl.repository.UserUrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TinyUrlService {

    private final UrlRepository urlRepository;
    private final UserUrlRepository userUrlRepository;
    private final UrlService urlService;
    private final CacheService cacheService;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final int MAX_COLLISION_RETRIES = 3;

    @Transactional
    public CreateUrlResult createOrGetShortUrl(String inputUrl, String userId) {
        String normalizedUrl = urlService.normalizeUrl(inputUrl);
        String userIdLower = userId.toLowerCase();

        CacheService.CacheResult cacheResult = cacheService.getCodeByUrl(normalizedUrl);
        if (cacheResult.value != null) {
            String cachedCode = cacheResult.value;
            Optional<Url> existingUrl = urlRepository.findById(cachedCode);
            if (existingUrl.isPresent()) {
                boolean alreadyAssociated = userUrlRepository.existsByUserIdLowerAndCode(userIdLower, cachedCode);
                if (!alreadyAssociated) {
                    UserUrl userUrl = new UserUrl();
                    userUrl.setUserIdLower(userIdLower);
                    userUrl.setCode(cachedCode);
                    userUrlRepository.save(userUrl);
                }
                return new CreateUrlResult(cachedCode, buildShortUrl(cachedCode), normalizedUrl,
                    existingUrl.get().getCreatedAt(), true);
            }
        }

        Optional<Url> existingUrl = urlRepository.findByNormalizedUrl(normalizedUrl);
        if (existingUrl.isPresent()) {
            String code = existingUrl.get().getCode();
            boolean alreadyAssociated = userUrlRepository.existsByUserIdLowerAndCode(userIdLower, code);
            if (!alreadyAssociated) {
                UserUrl userUrl = new UserUrl();
                userUrl.setUserIdLower(userIdLower);
                userUrl.setCode(code);
                userUrlRepository.save(userUrl);
            }
            cacheService.putBidirectional(code, normalizedUrl);
            return new CreateUrlResult(code, buildShortUrl(code), normalizedUrl,
                existingUrl.get().getCreatedAt(), true);
        }

        String newCode = generateUniqueCode();
        Url newUrl = new Url();
        newUrl.setCode(newCode);
        newUrl.setNormalizedUrl(normalizedUrl);
        urlRepository.save(newUrl);

        UserUrl userUrl = new UserUrl();
        userUrl.setUserIdLower(userIdLower);
        userUrl.setCode(newCode);
        userUrlRepository.save(userUrl);
        cacheService.putBidirectional(newCode, normalizedUrl);

        log.info("Created new short URL: {} -> {}", newCode, normalizedUrl);
        return new CreateUrlResult(newCode, buildShortUrl(newCode), normalizedUrl,
            newUrl.getCreatedAt(), false);
    }

    public Optional<String> getUrlByCode(String code) {
        String normalizedCode = urlService.normalizeShortCode(code);
        if (!urlService.isValidShortCode(normalizedCode)) {
            return Optional.empty();
        }

        CacheService.CacheResult urlCacheResult = cacheService.getUrlByCode(normalizedCode);
        if (urlCacheResult.value != null) {
            return Optional.of(urlCacheResult.value);
        }

        Optional<Url> url = urlRepository.findById(normalizedCode);
        if (url.isPresent()) {
            String normalizedUrl = url.get().getNormalizedUrl();
            cacheService.putBidirectional(normalizedCode, normalizedUrl);
            return Optional.of(normalizedUrl);
        }

        return Optional.empty();
    }

    public List<UserUrl> getUserUrls(String userId) {
        String userIdLower = userId.toLowerCase();
        return userUrlRepository.findByUserIdLowerOrderByCreatedAtDesc(userIdLower);
    }

    @Transactional
    public boolean deleteUserUrlAssociation(String userId, String code) {
        String userIdLower = userId.toLowerCase();
        String normalizedCode = urlService.normalizeShortCode(code);

        if (userUrlRepository.existsByUserIdLowerAndCode(userIdLower, normalizedCode)) {
            userUrlRepository.deleteByUserIdLowerAndCode(userIdLower, normalizedCode);
            return true;
        }
        return false;
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_COLLISION_RETRIES; attempt++) {
            String code = urlService.generateShortCode();

            try {
                if (!urlRepository.existsByCode(code)) {
                    return code;
                }
                log.debug("Code collision detected for: {}, attempt: {}", code, attempt + 1);
            } catch (Exception e) {
                log.warn("Error checking code existence: {}, attempt: {}", code, attempt + 1, e);
            }
        }
        throw new RuntimeException("COLLISION_RETRY_EXHAUSTED");
    }

    private String buildShortUrl(String code) {
        return baseUrl + "/" + code;
    }

    public static class CreateUrlResult {
        public final String code;
        public final String shortUrl;
        public final String url;
        public final java.time.LocalDateTime createdAt;
        public final boolean existed;

        public CreateUrlResult(String code, String shortUrl, String url,
                              java.time.LocalDateTime createdAt, boolean existed) {
            this.code = code;
            this.shortUrl = shortUrl;
            this.url = url;
            this.createdAt = createdAt;
            this.existed = existed;
        }
    }
}