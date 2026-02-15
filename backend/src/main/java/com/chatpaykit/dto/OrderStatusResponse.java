package com.chatpaykit.dto;

import java.time.Instant;
import java.util.UUID;

import com.chatpaykit.entity.Order;
import com.chatpaykit.entity.OrderStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderStatusResponse {

    private UUID id;
    private OrderStatus status;

    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpayRefundId;

    private Instant createdAt;
    private Instant updatedAt;

    private Instant verifiedAt;
    private Instant paidAt;
    private Instant failedAt;
    private Instant refundedAt;

    // ✅ Retry + debugging visibility
    private Integer attemptCount;
    private String lastError;

    public static OrderStatusResponse from(Order o) {
        return OrderStatusResponse.builder()
                .id(o.getId())
                .status(o.getStatus())
                .razorpayOrderId(o.getRazorpayOrderId())
                .razorpayPaymentId(o.getRazorpayPaymentId())
                .razorpayRefundId(o.getRazorpayRefundId())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .verifiedAt(o.getVerifiedAt())
                .paidAt(o.getPaidAt())
                .failedAt(o.getFailedAt())
                .refundedAt(o.getRefundedAt())
                .attemptCount(o.getAttemptCount())
                .lastError(o.getLastError())
                .build();
    }
}
