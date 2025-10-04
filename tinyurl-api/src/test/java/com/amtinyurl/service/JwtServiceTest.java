package com.amtinyurl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("1fe2275ec12ed522e57b743c64facf12");
    }

    @Test
    void shouldGenerateAndValidateToken() {
        String userId = "AbC123";
        String token = jwtService.generateToken(userId);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        String validatedUserId = jwtService.validateTokenAndGetUserId(token);
        assertEquals("abc123", validatedUserId);
    }

    @Test
    void shouldReturnNullForInvalidToken() {
        String invalidToken = "invalid.token.here";
        String result = jwtService.validateTokenAndGetUserId(invalidToken);
        assertNull(result);
    }

    @Test
    void shouldValidateUserIdFormat() {
        assertTrue(jwtService.isValidUserId("abc123"));
        assertTrue(jwtService.isValidUserId("ABC123"));
        assertTrue(jwtService.isValidUserId("a1b2c3"));

        assertFalse(jwtService.isValidUserId("abc12"));  // too short
        assertFalse(jwtService.isValidUserId("abc1234")); // too long
        assertFalse(jwtService.isValidUserId("abc@23"));  // invalid character
        assertFalse(jwtService.isValidUserId(null));      // null
    }
}