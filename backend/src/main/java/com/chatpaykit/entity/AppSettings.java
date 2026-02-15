package com.chatpaykit.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "app_settings")
@Getter
@Setter
public class AppSettings {

    @Id
    private Long id = 1L; // single row

    @Column(name = "razorpay_key_id")
    private String razorpayKeyId;

    @Column(name = "razorpay_key_secret")
    private String razorpayKeySecret;

    @Column(name = "razorpay_webhook_secret")   
    private String razorpayWebhookSecret;

    @Column(name = "whatsapp_access_token", columnDefinition = "TEXT")
    private String whatsappAccessToken;

    @Column(name = "whatsapp_phone_number_id")
    private String whatsappPhoneNumberId;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
