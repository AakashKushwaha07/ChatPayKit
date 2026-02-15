package com.chatpaykit.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RefundRequest {
    // paise; allow partial refund too
    @NotNull
    @Min(1)
    private Long amountPaise;
}
