package com.chatpaykit.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyPaymentRequest {
    @NotBlank
    private String razorpayOrderId;

    @NotBlank
    private String razorpayPaymentId;

    @NotBlank
    private String razorpaySignature;
}
