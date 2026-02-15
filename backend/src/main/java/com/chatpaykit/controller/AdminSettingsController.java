package com.chatpaykit.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chatpaykit.dto.TenantSettingsRequest;
import com.chatpaykit.dto.TenantSettingsResponse;
import com.chatpaykit.service.AppSettingsService;
import com.chatpaykit.service.JwtService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final AppSettingsService appSettingsService;
    private final JwtService jwtService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public TenantSettingsResponse get(@RequestHeader("Authorization") String authHeader) {
        UUID tenantId = jwtService.extractTenantId(extractToken(authHeader));

        // ✅ IMPORTANT: first-time tenant may not have settings yet -> return empty, not 500
        return appSettingsService.getOrNull(tenantId)
                .map(TenantSettingsResponse::from)
                .orElseGet(TenantSettingsResponse::empty);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public TenantSettingsResponse save(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody TenantSettingsRequest req
    ) {
        UUID tenantId = jwtService.extractTenantId(extractToken(authHeader));
        return TenantSettingsResponse.from(appSettingsService.upsert(tenantId, req));
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalStateException("Missing or invalid Authorization header");
        }
        return authHeader.substring(7).trim();
    }
}
