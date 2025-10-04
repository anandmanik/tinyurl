package com.amtinyurl.controller;

import com.amtinyurl.dto.TokenRequest;
import com.amtinyurl.dto.TokenResponse;
import com.amtinyurl.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final JwtService jwtService;

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> generateToken(@Valid @RequestBody TokenRequest request) {
        String userId = request.getUserId();
        String userIdLower = userId.toLowerCase();
        String token = jwtService.generateToken(userId);

        log.info("Generated token for userId: {}", userIdLower);

        return ResponseEntity.ok(new TokenResponse(token, userIdLower));
    }
}