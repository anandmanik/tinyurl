package com.amtinyurl.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;

@Service
@Slf4j
public class UrlService {

    private static final int MAX_URL_LENGTH = 2048;
    private static final String BASE36_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 7;
    private static final SecureRandom random = new SecureRandom();

    public String normalizeUrl(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        String trimmed = urlString.trim();

        if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) {
            trimmed = "https://" + trimmed;
        }

        if (!trimmed.startsWith("https://")) {
            throw new IllegalArgumentException("Only HTTPS URLs are allowed");
        }

        if (trimmed.length() > MAX_URL_LENGTH) {
            throw new IllegalArgumentException("URL length exceeds maximum allowed length of " + MAX_URL_LENGTH);
        }

        try {
            URL url = new URL(trimmed);

            if (url.getHost().toLowerCase().contains("amtinyurl.com")) {
                throw new IllegalArgumentException("URLs pointing to amtinyurl.com are not allowed to prevent loops");
            }

            String normalizedHost = url.getHost().toLowerCase();
            String scheme = url.getProtocol().toLowerCase();
            String path = url.getPath();
            String query = url.getQuery();
            String fragment = url.getRef();

            StringBuilder normalized = new StringBuilder();
            normalized.append(scheme).append("://").append(normalizedHost);

            if (url.getPort() != -1 && url.getPort() != url.getDefaultPort()) {
                normalized.append(":").append(url.getPort());
            }

            if (path != null && !path.isEmpty()) {
                normalized.append(path);
            }

            if (query != null && !query.isEmpty()) {
                normalized.append("?").append(query);
            }

            if (fragment != null && !fragment.isEmpty()) {
                normalized.append("#").append(fragment);
            }

            return normalized.toString();

        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL format: " + e.getMessage());
        }
    }

    public String generateShortCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(BASE36_CHARS.charAt(random.nextInt(BASE36_CHARS.length())));
        }
        return code.toString();
    }

    public boolean isValidShortCode(String code) {
        if (code == null || code.length() != CODE_LENGTH) {
            return false;
        }

        for (char c : code.toCharArray()) {
            if (BASE36_CHARS.indexOf(Character.toLowerCase(c)) == -1) {
                return false;
            }
        }

        return true;
    }

    public String normalizeShortCode(String code) {
        return code == null ? null : code.toLowerCase();
    }
}