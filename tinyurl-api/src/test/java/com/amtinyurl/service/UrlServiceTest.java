package com.amtinyurl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UrlServiceTest {

    private UrlService urlService;

    @BeforeEach
    void setUp() {
        urlService = new UrlService();
    }

    @Test
    void shouldNormalizeUrlCorrectly() {
        assertEquals("https://example.com/path?query=1",
            urlService.normalizeUrl("https://Example.com/path?query=1"));

        assertEquals("https://example.com/path",
            urlService.normalizeUrl("example.com/path"));

        assertEquals("https://example.com",
            urlService.normalizeUrl("EXAMPLE.COM"));
    }

    @Test
    void shouldRejectHttpUrls() {
        assertThrows(IllegalArgumentException.class,
            () -> urlService.normalizeUrl("http://example.com"));
    }

    @Test
    void shouldRejectAmtinyurlUrls() {
        assertThrows(IllegalArgumentException.class,
            () -> urlService.normalizeUrl("https://amtinyurl.com/test"));

        assertThrows(IllegalArgumentException.class,
            () -> urlService.normalizeUrl("https://sub.amtinyurl.com/test"));
    }

    @Test
    void shouldRejectTooLongUrls() {
        String longUrl = "https://example.com/" + "a".repeat(2048);
        assertThrows(IllegalArgumentException.class,
            () -> urlService.normalizeUrl(longUrl));
    }

    @Test
    void shouldGenerateValidShortCodes() {
        String code = urlService.generateShortCode();
        assertNotNull(code);
        assertEquals(7, code.length());
        assertTrue(urlService.isValidShortCode(code));
    }

    @Test
    void shouldValidateShortCodes() {
        assertTrue(urlService.isValidShortCode("abc1234"));
        assertTrue(urlService.isValidShortCode("1234567"));
        assertTrue(urlService.isValidShortCode("abcdefg"));

        assertFalse(urlService.isValidShortCode("abc123"));   // too short
        assertFalse(urlService.isValidShortCode("abc12345")); // too long
        assertFalse(urlService.isValidShortCode("abc@234"));  // invalid character
        assertFalse(urlService.isValidShortCode(null));       // null
    }

    @Test
    void shouldNormalizeShortCodes() {
        assertEquals("abc1234", urlService.normalizeShortCode("ABC1234"));
        assertEquals("abcdefg", urlService.normalizeShortCode("AbCdEfG"));
        assertNull(urlService.normalizeShortCode(null));
    }
}