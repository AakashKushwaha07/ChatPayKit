package com.chatpaykit.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("Missing app.jwt.secret");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UUID userId, UUID tenantId, String role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(60L * 60L * 24L * 7L); // 7 days (MVP)

        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .claim("userId", userId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("role", role)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public UUID extractTenantId(String token) {
        Object v = parseClaims(token).get("tenantId");
        if (v == null) throw new IllegalStateException("tenantId missing in JWT");
        return UUID.fromString(String.valueOf(v));
    }

    public UUID extractUserId(String token) {
        Object v = parseClaims(token).get("userId");
        if (v == null) throw new IllegalStateException("userId missing in JWT");
        return UUID.fromString(String.valueOf(v));
    }

    public String extractRole(String token) {
        Object v = parseClaims(token).get("role");
        if (v == null) throw new IllegalStateException("role missing in JWT");
        return String.valueOf(v);
    }
}
