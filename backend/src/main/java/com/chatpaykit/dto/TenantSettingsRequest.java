package com.chatpaykit.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TenantSettingsRequest {
    private String razorpayKeyId;
    private String razorpayKeySecret;
    private String razorpayWebhookSecret;
    private String whatsappAccessToken;
    private String whatsappPhoneNumberId;
}
