package com.chatpaykit.service;

import java.util.List;
import java.util.UUID;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.chatpaykit.entity.Order;
import com.razorpay.RazorpayClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RazorpayPaymentService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentService.class);

    private final AppSettingsService appSettingsService;
    private final WhatsappPaymentService whatsappPaymentService;

    // ✅ Multi-tenant: Razorpay client per tenant
    private RazorpayClient clientOrThrow(UUID tenantId) {
        var s = appSettingsService.getOrThrow(tenantId);

        String keyId = s.getRazorpayKeyId();
        String keySecret = s.getRazorpayKeySecret();

        if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
            throw new IllegalStateException("Razorpay keys missing for tenantId=" + tenantId);
        }

        try {
            return new RazorpayClient(keyId, keySecret);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to init RazorpayClient: " + e.getMessage(), e);
        }
    }

    // ---------------------------
    // Create Razorpay Order (base)
    // ---------------------------
    public String createRazorpayOrder(Order order) {
        RazorpayClient razorpayClient = clientOrThrow(order.getTenantId());

        try {
            JSONObject req = new JSONObject();
            req.put("amount", order.getAmountPaise());
            req.put("currency", order.getCurrency());

            String shortReceipt = "cpk_" + order.getId().toString().replace("-", "").substring(0, 12);
            req.put("receipt", shortReceipt);

            JSONObject notes = new JSONObject();
            notes.put("customer", order.getCustomerName());
            notes.put("whatsapp", order.getCustomerWhatsapp());
            req.put("notes", notes);

            log.info("Creating Razorpay order internalOrderId={} tenantId={} amountPaise={} currency={}",
                    order.getId(), order.getTenantId(), order.getAmountPaise(), order.getCurrency());

            com.razorpay.Order rzpOrder = razorpayClient.orders.create(req);
            String razorpayOrderId = rzpOrder.get("id");

            log.info("Razorpay order created internalOrderId={} tenantId={} razorpayOrderId={}",
                    order.getId(), order.getTenantId(), razorpayOrderId);

            return razorpayOrderId;

        } catch (Exception e) {
            log.error("Failed to create Razorpay order internalOrderId={} tenantId={} msg={}",
                    order.getId(), order.getTenantId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create Razorpay order: " + e.getMessage(), e);
        }
    }

    // ---------------------------
    // Retry Razorpay Order
    // ---------------------------
    public String createRazorpayOrderForRetry(Order order) {
        RazorpayClient razorpayClient = clientOrThrow(order.getTenantId());

        try {
            JSONObject req = new JSONObject();
            req.put("amount", order.getAmountPaise());
            req.put("currency", order.getCurrency());

            String shortReceipt = "cpk_retry_" + order.getId().toString().replace("-", "").substring(0, 10)
                    + "_" + System.currentTimeMillis();
            req.put("receipt", shortReceipt);

            JSONObject notes = new JSONObject();
            notes.put("customer", order.getCustomerName());
            notes.put("whatsapp", order.getCustomerWhatsapp());
            notes.put("retry", true);
            notes.put("internalOrderId", order.getId().toString());
            req.put("notes", notes);

            log.info("Creating Razorpay retry order internalOrderId={} tenantId={} attemptCount={}",
                    order.getId(), order.getTenantId(), order.getAttemptCount());

            com.razorpay.Order rzpOrder = razorpayClient.orders.create(req);
            String razorpayOrderId = rzpOrder.get("id");

            log.info("Razorpay retry order created internalOrderId={} tenantId={} razorpayOrderId={}",
                    order.getId(), order.getTenantId(), razorpayOrderId);

            return razorpayOrderId;

        } catch (Exception e) {
            log.error("Failed to create Razorpay retry order internalOrderId={} tenantId={} msg={}",
                    order.getId(), order.getTenantId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create Razorpay retry order: " + e.getMessage(), e);
        }
    }

    // ---------------------------
    // ✅ Send WhatsApp Payment Request
    // ---------------------------
    public String sendPaymentRequestOnWhatsapp(Order order) {
        String razorpayOrderId = createRazorpayOrder(order);

        try {
            whatsappPaymentService.sendInChatPaymentRequest(order, razorpayOrderId);
        } catch (Exception e) {
            log.warn("WhatsApp payment request failed internalOrderId={} tenantId={} msg={}",
                    order.getId(), order.getTenantId(), e.getMessage(), e);
        }

        return razorpayOrderId;
    }

    // ---------------------------
    // ✅ Send WhatsApp Retry Payment Request
    // ---------------------------
    public String sendRetryPaymentRequestOnWhatsapp(Order order) {
        String razorpayOrderId = createRazorpayOrderForRetry(order);

        try {
            whatsappPaymentService.sendInChatPaymentRequest(order, razorpayOrderId);
        } catch (Exception e) {
            log.warn("WhatsApp retry payment request failed internalOrderId={} tenantId={} msg={}",
                    order.getId(), order.getTenantId(), e.getMessage(), e);
        }

        return razorpayOrderId;
    }

    // ---------------------------
    // Refund helper (full refund)
    // ---------------------------
    public String refundPayment(UUID tenantId, String razorpayPaymentId) {
        RazorpayClient razorpayClient = clientOrThrow(tenantId);

        try {
            log.info("Creating Razorpay refund tenantId={} paymentId={}", tenantId, razorpayPaymentId);

            JSONObject req = new JSONObject();
            req.put("payment_id", razorpayPaymentId);

            com.razorpay.Refund refund = razorpayClient.payments.refund(req);

            String refundId = refund.get("id");
            log.info("Refund created tenantId={} paymentId={} refundId={}", tenantId, razorpayPaymentId, refundId);

            return refundId;

        } catch (Exception e) {
            log.error("Refund failed tenantId={} paymentId={} msg={}", tenantId, razorpayPaymentId, e.getMessage(), e);
            throw new RuntimeException("Refund failed: " + e.getMessage(), e);
        }
    }

    // ---------------------------
    // Sync helpers (used by OrderSyncController)
    // ---------------------------
    public JSONObject fetchRefund(UUID tenantId, String refundId) {
        try {
            return clientOrThrow(tenantId).refunds.fetch(refundId).toJson();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch refund: " + e.getMessage(), e);
        }
    }

    public JSONObject fetchPayment(UUID tenantId, String paymentId) {
        try {
            return clientOrThrow(tenantId).payments.fetch(paymentId).toJson();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch payment: " + e.getMessage(), e);
        }
    }

    public List<JSONObject> fetchPaymentsByOrderId(UUID tenantId, String razorpayOrderId) {
        try {
            var payments = clientOrThrow(tenantId).orders.fetchPayments(razorpayOrderId);
            return payments.stream().map(p -> p.toJson()).toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch payments for order: " + e.getMessage(), e);
        }
    }
}
