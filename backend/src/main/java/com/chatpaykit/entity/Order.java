package com.chatpaykit.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
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
@Table(name = "orders")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue
    private UUID id;

    // ✅ NEW: Multi-tenant support
    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false, length = 20)
    private String customerWhatsapp;

    @Column(nullable = false)
    private Long amountPaise;

    @Column(nullable = false, length = 8)
    private String currency;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(length = 64)
    private String razorpayOrderId;

    @Column(length = 64)
    private String razorpayPaymentId;

    @Column(length = 64)
    private String razorpayRefundId;

    private Instant refundedAt;
    private Instant verifiedAt;
    private Instant paidAt;
    private Instant failedAt;

    @Column(nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(length = 1000)
    private String lastError;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(length = 64)
    private String whatsappPaymentReferenceId;

    private Instant paidMsgSentAt;
    private Instant failedMsgSentAt;
    private Instant refundedMsgSentAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = OrderStatus.CREATED;
        if (this.currency == null) this.currency = "INR";
        if (this.attemptCount == null) this.attemptCount = 0;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
