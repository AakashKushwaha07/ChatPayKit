package com.chatpaykit.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AuthSignupRequest {
    @NotBlank
    private String tenantName;

    @Email @NotBlank
    private String email;

    @NotBlank
    private String password;
}
