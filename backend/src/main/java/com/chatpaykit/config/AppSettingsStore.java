package com.chatpaykit.config;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

@Component
public class AppSettingsStore {

    public record Settings(
            String razorpayKeyId,
            String razorpayKeySecret,
            String whatsappAccessToken,
            String whatsappPhoneNumberId
    ) {}

    private final AtomicReference<Settings> ref =
            new AtomicReference<>(new Settings("", "", "", ""));

    public Settings get() {
        return ref.get();
    }

    public void set(Settings s) {
        ref.set(s);
    }

    public boolean isConfigured() {
        Settings s = ref.get();
        return notBlank(s.razorpayKeyId())
                && notBlank(s.razorpayKeySecret())
                && notBlank(s.whatsappAccessToken())
                && notBlank(s.whatsappPhoneNumberId());
    }

    private boolean notBlank(String v) {
        return v != null && !v.trim().isEmpty();
    }
}
