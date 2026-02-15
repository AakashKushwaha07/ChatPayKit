package com.chatpaykit.controller;

import java.time.Instant;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chatpaykit.entity.Order;
import com.chatpaykit.entity.OrderStatus;
import com.chatpaykit.entity.WebhookEvent;
import com.chatpaykit.repository.OrderRepository;
import com.chatpaykit.repository.WebhookEventRepository;
import com.chatpaykit.service.AppSettingsService;
import com.chatpaykit.service.WhatsappPaymentService;
import com.chatpaykit.util.RazorpaySignatureUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/webhooks/razorpay")
@RequiredArgsConstructor
public class RazorpayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookController.class);

    private final OrderRepository orderRepository;
    private final WebhookEventRepository webhookEventRepository;

    private final WhatsappPaymentService whatsappPaymentService;
    private final AppSettingsService appSettingsService;

    @PostMapping
    public ResponseEntity<?> handle(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature
    ) {
        try {
            if (signature == null || signature.isBlank()) {
                return ResponseEntity.badRequest().body("Missing X-Razorpay-Signature header");
            }

            JSONObject json = new JSONObject(payload);
            String eventType = json.optString("event", "");
            if (eventType.isBlank()) {
                return ResponseEntity.badRequest().body("Missing event type");
            }

            // Idempotency key: prefer Razorpay webhook "id"
            String eventId = json.optString("id", "");
            if (eventId.isBlank()) {
                String createdAt = String.valueOf(json.optLong("created_at", 0));
                eventId = "fallback|" + eventType + "|" + createdAt;
            }

            if (webhookEventRepository.existsById(eventId)) {
                return ResponseEntity.ok("Already processed");
            }

            // Payment entity (capture/fail events)
            JSONObject paymentEntity = json.optJSONObject("payload")
                    .optJSONObject("payment")
                    .optJSONObject("entity");

            // Refund entity (refunded event)
            JSONObject refundEntity = json.optJSONObject("payload")
                    .optJSONObject("refund")
                    .optJSONObject("entity");

            // Identify Razorpay orderId / paymentId (try multiple places)
            String razorpayOrderId = "";
            String razorpayPaymentId = "";

            if (paymentEntity != null) {
                razorpayOrderId = paymentEntity.optString("order_id", "");
                razorpayPaymentId = paymentEntity.optString("id", "");
            }
            if (razorpayPaymentId.isBlank() && refundEntity != null) {
                razorpayPaymentId = refundEntity.optString("payment_id", "");
            }

            // Resolve Order:
            Order order = null;

            if (!razorpayOrderId.isBlank()) {
                order = orderRepository.findByRazorpayOrderId(razorpayOrderId).orElse(null);
            }
            // fallback by paymentId
            if (order == null && !razorpayPaymentId.isBlank()) {
                order = orderRepository.findByRazorpayPaymentId(razorpayPaymentId).orElse(null);
            }

            // ✅ Multi-tenant: if we cannot resolve order → we cannot know which secret to verify with
            if (order == null) {
                webhookEventRepository.save(WebhookEvent.builder().eventId(eventId).processedAt(Instant.now()).build());
                return ResponseEntity.ok("Order not found (ignored)");
            }

            // ✅ Tenant-specific webhook secret
            String webhookSecret;
            try {
                webhookSecret = appSettingsService.getOrThrow(order.getTenantId()).getRazorpayWebhookSecret();
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Webhook secret not configured for tenant. Save it in Settings.");
            }

            if (webhookSecret == null || webhookSecret.isBlank()) {
                return ResponseEntity.badRequest().body("Webhook secret missing for tenant. Save it in Settings.");
            }

            // ✅ Verify signature AFTER resolving tenant
            if (!RazorpaySignatureUtil.verify(payload, signature, webhookSecret)) {
                return ResponseEntity.status(401).body("Invalid signature");
            }

            // Store paymentId always if present
            if (!razorpayPaymentId.isBlank()
                    && (order.getRazorpayPaymentId() == null || order.getRazorpayPaymentId().isBlank())) {
                order.setRazorpayPaymentId(razorpayPaymentId);
            }

            // Decide status transition
            OrderStatus next;

            if ("payment.captured".equalsIgnoreCase(eventType)) {
                next = OrderStatus.PAID;
            } else if ("payment.failed".equalsIgnoreCase(eventType)) {
                next = OrderStatus.FAILED;
            } else if ("payment.refunded".equalsIgnoreCase(eventType)) {
                next = OrderStatus.REFUNDED;
            } else {
                webhookEventRepository.save(WebhookEvent.builder().eventId(eventId).processedAt(Instant.now()).build());
                return ResponseEntity.ok("Event ignored: " + eventType);
            }

            // Safe transitions (no downgrade)
            OrderStatus current = order.getStatus();
            if (current != null && !current.canTransitionTo(next)) {
                log.warn("Blocked invalid transition orderId={} {} -> {} event={}",
                        order.getId(), current, next, eventType);

                webhookEventRepository.save(WebhookEvent.builder().eventId(eventId).processedAt(Instant.now()).build());
                return ResponseEntity.ok("Transition blocked (ignored)");
            }

            // Apply updates
            order.setStatus(next);

            if (next == OrderStatus.PAID) {
                if (order.getPaidAt() == null) order.setPaidAt(Instant.now());
                order.setFailedAt(null);
                order.setLastError(null);

            } else if (next == OrderStatus.FAILED) {
                if (order.getFailedAt() == null) order.setFailedAt(Instant.now());

            } else if (next == OrderStatus.REFUNDED) {
                if (refundEntity != null) {
                    String refundId = refundEntity.optString("id", "");
                    if (!refundId.isBlank()) order.setRazorpayRefundId(refundId);
                }
                if (order.getRefundedAt() == null) order.setRefundedAt(Instant.now());
            }

            // WhatsApp auto message (only once)
            sendWhatsappOnce(order, next);

            orderRepository.save(order);
            webhookEventRepository.save(WebhookEvent.builder().eventId(eventId).processedAt(Instant.now()).build());

            return ResponseEntity.ok("Processed: " + eventType);

        } catch (Exception e) {
            log.error("Webhook handler error", e);
            // Keep 200 to avoid retry storm
            return ResponseEntity.ok("Webhook handler error (logged)");
        }
    }

    private void sendWhatsappOnce(Order order, OrderStatus next) {
        try {
            if (next == OrderStatus.PAID && order.getPaidMsgSentAt() == null) {
                whatsappPaymentService.sendPaymentSuccess(order);
                order.setPaidMsgSentAt(Instant.now());
                return;
            }

            if (next == OrderStatus.FAILED && order.getFailedMsgSentAt() == null) {
                whatsappPaymentService.sendPaymentFailed(order);
                order.setFailedMsgSentAt(Instant.now());
                return;
            }

            if (next == OrderStatus.REFUNDED && order.getRefundedMsgSentAt() == null) {
                whatsappPaymentService.sendRefunded(order);
                order.setRefundedMsgSentAt(Instant.now());
                return;
            }
        } catch (Exception ex) {
            log.warn("WhatsApp send failed orderId={} next={} msg={}",
                    order.getId(), next, ex.getMessage());
            order.setLastError("WhatsApp send failed: " + ex.getMessage());
        }
    }
}
