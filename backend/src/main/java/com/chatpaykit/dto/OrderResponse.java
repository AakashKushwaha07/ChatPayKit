package com.chatpaykit.dto;

import java.time.Instant;
import java.util.UUID;

import com.chatpaykit.entity.Order;
import com.chatpaykit.entity.OrderStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderResponse {
    private UUID id;

    private String customerName;
    private String customerWhatsapp;

    private Long amountPaise;
    private String currency;
    private String description;

    private OrderStatus status;

    private String razorpayOrderId;
    private String razorpayPaymentId;

    // ✅ Refund fields
    private String razorpayRefundId;
    private Instant refundedAt;

    // ✅ Lifecycle timestamps
    private Instant verifiedAt;
    private Instant paidAt;
    private Instant failedAt;

    // ✅ Retry + debug
    private Integer attemptCount;
    private String lastError;

    private String whatsappPaymentReferenceId;

    private Instant createdAt;
    private Instant updatedAt;

    public static OrderResponse from(Order o) {
        return OrderResponse.builder()
                .id(o.getId())
                .customerName(o.getCustomerName())
                .customerWhatsapp(o.getCustomerWhatsapp())
                .amountPaise(o.getAmountPaise())
                .currency(o.getCurrency())
                .description(o.getDescription())
                .status(o.getStatus())
                .razorpayOrderId(o.getRazorpayOrderId())
                .razorpayPaymentId(o.getRazorpayPaymentId())
                .razorpayRefundId(o.getRazorpayRefundId())
                .refundedAt(o.getRefundedAt())
                .verifiedAt(o.getVerifiedAt())
                .paidAt(o.getPaidAt())
                .failedAt(o.getFailedAt())
                .attemptCount(o.getAttemptCount())
                .lastError(o.getLastError())
                .whatsappPaymentReferenceId(o.getWhatsappPaymentReferenceId())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}
