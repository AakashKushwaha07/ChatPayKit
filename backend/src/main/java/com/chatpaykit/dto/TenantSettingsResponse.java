package com.chatpaykit.dto;

import com.chatpaykit.entity.TenantSettings;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TenantSettingsResponse {

    private String razorpayKeyId;
    private String razorpayKeySecret;
    private String razorpayWebhookSecret;
    private String whatsappAccessToken;
    private String whatsappPhoneNumberId;

    public static TenantSettingsResponse from(TenantSettings s) {
        return new TenantSettingsResponse(
                s.getRazorpayKeyId(),
                s.getRazorpayKeySecret(),
                s.getRazorpayWebhookSecret(),
                s.getWhatsappAccessToken(),
                s.getWhatsappPhoneNumberId()
        );
    }

    // ✅ ADD THIS METHOD
    public static TenantSettingsResponse empty() {
        return new TenantSettingsResponse(
                "",
                "",
                "",
                "",
                ""
        );
    }
}
