package com.chatpaykit.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chatpaykit.dto.TenantSettingsRequest;
import com.chatpaykit.entity.TenantSettings;
import com.chatpaykit.repository.TenantSettingsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppSettingsService {

    private final TenantSettingsRepository tenantSettingsRepository;

    // ✅ NEW: safe getter (no 500 on first-time tenant)
    @Transactional(readOnly = true)
    public Optional<TenantSettings> getOrNull(UUID tenantId) {
        return tenantSettingsRepository.findById(tenantId);
    }

    @Transactional(readOnly = true)
    public TenantSettings getOrThrow(UUID tenantId) {
        return tenantSettingsRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException(
                        "Tenant settings not configured for tenantId=" + tenantId
                ));
    }

    @Transactional
    public TenantSettings upsert(UUID tenantId, TenantSettingsRequest req) {
        TenantSettings s = tenantSettingsRepository.findById(tenantId)
                .orElseGet(() -> TenantSettings.builder().tenantId(tenantId).build());

        s.setRazorpayKeyId(req.getRazorpayKeyId());
        s.setRazorpayKeySecret(req.getRazorpayKeySecret());
        s.setRazorpayWebhookSecret(req.getRazorpayWebhookSecret());
        s.setWhatsappAccessToken(req.getWhatsappAccessToken());
        s.setWhatsappPhoneNumberId(req.getWhatsappPhoneNumberId());

        return tenantSettingsRepository.save(s);
    }
}
