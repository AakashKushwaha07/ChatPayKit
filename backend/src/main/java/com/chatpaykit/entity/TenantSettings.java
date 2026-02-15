package com.chatpaykit.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tenant_settings")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TenantSettings {

    @Id
    private UUID tenantId; // PK

    @Column(length = 200)
    private String razorpayKeyId;

    @Column(length = 500)
    private String razorpayKeySecret;

    @Column(length = 500)
    private String whatsappAccessToken;

    @Column(length = 100)
    private String whatsappPhoneNumberId;

    @Column(length = 500)
    private String razorpayWebhookSecret;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
