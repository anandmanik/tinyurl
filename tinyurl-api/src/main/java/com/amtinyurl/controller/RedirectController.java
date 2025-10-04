package com.amtinyurl.controller;

import com.amtinyurl.service.TinyUrlService;
import com.amtinyurl.service.UrlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
public class RedirectController {

    private final TinyUrlService tinyUrlService;
    private final UrlService urlService;

    @GetMapping("/{code}")
    public ResponseEntity<?> redirect(@PathVariable String code) {
        String normalizedCode = urlService.normalizeShortCode(code);

        if (!urlService.isValidShortCode(normalizedCode)) {
            log.warn("Invalid short code format: {}", code);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "error", "Short code not found",
                    "code", "NOT_FOUND"
                ));
        }

        Optional<String> url = tinyUrlService.getUrlByCode(normalizedCode);

        if (url.isPresent()) {
            log.info("Redirecting {} -> {}", normalizedCode, url.get());

            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", url.get());
            headers.add("Cache-Control", "max-age=100, public");

            return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .headers(headers)
                .build();
        } else {
            log.warn("Short code not found: {}", normalizedCode);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "error", "Short code not found",
                    "code", "NOT_FOUND"
                ));
        }
    }
}