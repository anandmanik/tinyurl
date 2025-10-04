package com.amtinyurl.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
@Slf4j
public class JwtService {

    private final SecretKey secretKey;
    private final String issuer = "amtinyurl";

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String userId) {
        String userIdLower = userId.toLowerCase();
        return Jwts.builder()
                .setSubject(userIdLower)
                .setIssuer(issuer)
                .setIssuedAt(new Date())
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String validateTokenAndGetUserId(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String subject = claims.getSubject();
            String tokenIssuer = claims.getIssuer();

            if (!issuer.equals(tokenIssuer)) {
                log.warn("Invalid token issuer: {}", tokenIssuer);
                return null;
            }

            return subject;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return null;
        }
    }

    public boolean isValidUserId(String userId) {
        if (userId == null || userId.length() != 6) {
            return false;
        }
        return userId.matches("^[a-zA-Z0-9]{6}$");
    }
}