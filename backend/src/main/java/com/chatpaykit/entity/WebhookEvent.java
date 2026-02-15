package com.chatpaykit.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "webhook_events")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class WebhookEvent {

    @Id
    @Column(length = 120)
    private String eventId;

    @Column(length = 60)
    private String eventType;

    @Column(length = 50)
    private String razorpayOrderId;

    @Column(length = 50)
    private String razorpayPaymentId;

    @Column(nullable = false)
    private Instant processedAt;

    @PrePersist
    void onCreate() {
        if (processedAt == null) processedAt = Instant.now();
    }
}
