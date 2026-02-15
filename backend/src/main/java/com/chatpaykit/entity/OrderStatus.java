package com.chatpaykit.entity;

public enum OrderStatus {
    CREATED,
    PAYMENT_SENT,
    PAID,
    FAILED,
    EXPIRED,
    REFUND_PENDING,
    REFUNDED;

    // Production hardening: safe transitions + no downgrade
    public boolean canTransitionTo(OrderStatus next) {
        if (next == null) return false;
        if (this == next) return true;

        return switch (this) {
            case CREATED -> (next == PAYMENT_SENT || next == FAILED || next == EXPIRED);
            case PAYMENT_SENT -> (next == PAID || next == FAILED || next == EXPIRED);
            case PAID -> (next == REFUND_PENDING || next == REFUNDED);
            case FAILED -> (next == PAYMENT_SENT);       // retry allowed (FAILED -> PAYMENT_SENT)
            case EXPIRED -> (next == PAYMENT_SENT);                       // terminal (no retry)
            case REFUND_PENDING -> (next == REFUNDED);
            case REFUNDED -> false;                      // terminal
        };
    }

    public boolean isTerminal() {
        return this == EXPIRED || this == REFUNDED;
    }
}
