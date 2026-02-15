package com.chatpaykit.service;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.chatpaykit.entity.Order;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WhatsappPaymentService {

    private static final Logger log = LoggerFactory.getLogger(WhatsappPaymentService.class);

    private final RestClient restClient;
    private final AppSettingsService appSettingsService;

    private record WaConfig(String phoneNumberId, String accessToken) {}

    // ✅ Multi-tenant: config per tenant
    private WaConfig configOrNull(UUID tenantId) {
        try {
            var s = appSettingsService.getOrThrow(tenantId);
            String phoneNumberId = s.getWhatsappPhoneNumberId();
            String accessToken = s.getWhatsappAccessToken();

            if (phoneNumberId == null || phoneNumberId.isBlank()
                    || accessToken == null || accessToken.isBlank()) {
                return null;
            }
            return new WaConfig(phoneNumberId.trim(), accessToken.trim());
        } catch (Exception e) {
            // Not configured yet (or DB missing) -> skip
            return null;
        }
    }

    private String baseUrl(String phoneNumberId) {
        return "https://graph.facebook.com/v19.0/" + phoneNumberId + "/messages";
    }

    // -------- PAYMENT REQUEST --------
    public void sendInChatPaymentRequest(Order order, String razorpayOrderId) {
        WaConfig cfg = configOrNull(order.getTenantId());
        if (cfg == null) {
            log.warn("WhatsApp not configured for tenantId={}. Skipping send for orderId={}",
                    order.getTenantId(), order.getId());
            return;
        }

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", order.getCustomerWhatsapp(),
                "type", "interactive",
                "interactive", Map.of(
                        "type", "payment",
                        "payment", Map.of(
                                "provider", "razorpay",
                                "reference_id", "pay_" + order.getId(),
                                "amount", Map.of(
                                        "value", order.getAmountPaise(),
                                        "currency", order.getCurrency()
                                ),
                                "description", order.getDescription() == null ? "Order Payment" : order.getDescription(),
                                "metadata", Map.of(
                                        "razorpay_order_id", razorpayOrderId,
                                        "internal_order_id", order.getId().toString()
                                )
                        )
                )
        );

        send(cfg, payload);
    }

    // -------- SUCCESS --------
    public void sendPaymentSuccess(Order order) {
        WaConfig cfg = configOrNull(order.getTenantId());
        if (cfg == null) return;

        send(cfg, textPayload(order.getCustomerWhatsapp(),
                "✅ Payment received!\nOrder: " + order.getId()
                        + "\nAmount: ₹" + (order.getAmountPaise() / 100.0)));
    }

    // -------- FAILED --------
    public void sendPaymentFailed(Order order) {
        WaConfig cfg = configOrNull(order.getTenantId());
        if (cfg == null) return;

        send(cfg, textPayload(order.getCustomerWhatsapp(),
                "❌ Payment failed.\nOrder: " + order.getId()
                        + "\nPlease retry."));
    }

    // -------- REFUNDED --------
    public void sendRefunded(Order order) {
        WaConfig cfg = configOrNull(order.getTenantId());
        if (cfg == null) return;

        send(cfg, textPayload(order.getCustomerWhatsapp(),
                "💸 Refund processed.\nOrder: " + order.getId()));
    }

    private Map<String, Object> textPayload(String to, String msg) {
        return Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "text",
                "text", Map.of("body", msg)
        );
    }

    private void send(WaConfig cfg, Map<String, Object> payload) {
        try {
            restClient.post()
                    .uri(baseUrl(cfg.phoneNumberId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + cfg.accessToken())
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            // Keep behavior: don't crash business flow
            log.warn("WhatsApp send failed msg={}", e.getMessage());
        }
    }
}
