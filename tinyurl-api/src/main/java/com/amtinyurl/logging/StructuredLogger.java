package com.amtinyurl.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class StructuredLogger {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void logAction(String action, String code, String url, String cache, int status) {
        try {
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            logEntry.put("level", "INFO");
            logEntry.put("requestId", MDC.get("requestId"));
            logEntry.put("userId", MDC.get("userId"));
            logEntry.put("userAgent", MDC.get("userAgent"));
            logEntry.put("route", MDC.get("route"));
            logEntry.put("action", action);

            if (code != null) {
                logEntry.put("code", code);
            }
            if (url != null) {
                logEntry.put("url", url);
            }
            if (cache != null) {
                logEntry.put("cache", cache);
            }
            logEntry.put("status", status);

            String jsonLog = objectMapper.writeValueAsString(logEntry);
            log.info(jsonLog);
        } catch (Exception e) {
            log.error("Failed to write structured log", e);
        }
    }

    public void logAction(String action) {
        logAction(action, null, null, null, 200);
    }

    public void logAction(String action, int status) {
        logAction(action, null, null, null, status);
    }

    public void logAction(String action, String code, String url) {
        logAction(action, code, url, null, 200);
    }
}