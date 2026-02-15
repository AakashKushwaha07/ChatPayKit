package com.chatpaykit.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateOrderRequest {

    @NotBlank
    private String customerName;

    // WhatsApp expects countrycode + number often; keep simple for MVP
    @NotBlank
    @Pattern(regexp = "^[0-9]{10,20}$", message = "customerWhatsapp must be digits only, 10-20 length")
    private String customerWhatsapp;

    @NotNull
    @Min(value = 100, message = "amountPaise must be at least 100 (₹1)")
    private Long amountPaise;

    @Size(max = 500)
    private String description;
}
