package com.amtinyurl.controller;

import com.amtinyurl.dto.CreateUrlRequest;
import com.amtinyurl.dto.CreateUrlResponse;
import com.amtinyurl.dto.UrlListResponse;
import com.amtinyurl.entity.UserUrl;
import com.amtinyurl.service.TinyUrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class UrlController {

    private final TinyUrlService tinyUrlService;

    @Value("${app.base-url}")
    private String baseUrl;

    @PostMapping("/urls")
    public ResponseEntity<CreateUrlResponse> createUrl(@Valid @RequestBody CreateUrlRequest request,
                                                       Authentication authentication) {
        try {
            String userId = authentication.getName();
            TinyUrlService.CreateUrlResult result = tinyUrlService.createOrGetShortUrl(request.getUrl(), userId);

            CreateUrlResponse response = new CreateUrlResponse(
                result.code,
                result.shortUrl,
                result.url,
                result.createdAt,
                result.existed
            );

            HttpStatus status = result.existed ? HttpStatus.OK : HttpStatus.CREATED;
            log.info("URL creation/retrieval for user {}: {} -> {}, existed: {}",
                userId, request.getUrl(), result.code, result.existed);

            return ResponseEntity.status(status).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid URL provided: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            if ("COLLISION_RETRY_EXHAUSTED".equals(e.getMessage())) {
                log.error("Collision retries exhausted for URL: {}", request.getUrl());
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(null);
            }
            log.error("Unexpected error creating URL", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/urls")
    public ResponseEntity<List<UrlListResponse>> getUserUrls(Authentication authentication) {
        String userId = authentication.getName();
        List<UserUrl> userUrls = tinyUrlService.getUserUrls(userId);

        List<UrlListResponse> response = userUrls.stream()
            .map(userUrl -> new UrlListResponse(
                userUrl.getCode(),
                baseUrl + "/" + userUrl.getCode(),
                userUrl.getUrl().getNormalizedUrl(),
                userUrl.getCreatedAt()
            ))
            .collect(Collectors.toList());

        log.info("Retrieved {} URLs for user {}", response.size(), userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/urls/{code}")
    public ResponseEntity<Void> deleteUserUrl(@PathVariable String code, Authentication authentication) {
        String userId = authentication.getName();
        boolean deleted = tinyUrlService.deleteUserUrlAssociation(userId, code);

        if (deleted) {
            log.info("Deleted URL association for user {} and code {}", userId, code);
            return ResponseEntity.noContent().build();
        } else {
            log.warn("URL association not found for user {} and code {}", userId, code);
            return ResponseEntity.notFound().build();
        }
    }
}