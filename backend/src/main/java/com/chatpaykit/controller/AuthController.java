package com.chatpaykit.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chatpaykit.dto.AuthLoginRequest;
import com.chatpaykit.dto.AuthResponse;
import com.chatpaykit.dto.AuthSignupRequest;
import com.chatpaykit.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody AuthSignupRequest req) {
        String token = authService.signup(req);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthLoginRequest req) {
        String token = authService.login(req);
        return ResponseEntity.ok(new AuthResponse(token));
    }
}
