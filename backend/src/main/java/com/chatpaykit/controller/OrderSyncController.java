package com.chatpaykit.controller;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chatpaykit.entity.Order;
import com.chatpaykit.entity.OrderStatus;
import com.chatpaykit.repository.OrderRepository;
import com.chatpaykit.service.RazorpayPaymentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderSyncController {

    private final OrderRepository orderRepository;
    private final RazorpayPaymentService razorpayPaymentService;

    @GetMapping("/{id}/sync")
    public ResponseEntity<?> sync(@PathVariable UUID id) {

        Order order = orderRepository.findById(id).orElseThrow();

        try {

            UUID tenantId = order.getTenantId(); // ✅ multi-tenant

            // 1️⃣ Check refund first
            if (order.getRazorpayRefundId() != null && !order.getRazorpayRefundId().isBlank()) {

                JSONObject refundJson =
                        razorpayPaymentService.fetchRefund(tenantId, order.getRazorpayRefundId());

                String refundStatus = refundJson.optString("status", "");

                if ("processed".equalsIgnoreCase(refundStatus)) {
                    order.setStatus(OrderStatus.REFUNDED);
                    if (order.getRefundedAt() == null) {
                        order.setRefundedAt(Instant.now());
                    }
                } else if ("pending".equalsIgnoreCase(refundStatus)) {
                    order.setStatus(OrderStatus.REFUND_PENDING);
                }

                orderRepository.save(order);
                return ResponseEntity.ok(order);
            }

            // 2️⃣ If we have paymentId
            if (order.getRazorpayPaymentId() != null && !order.getRazorpayPaymentId().isBlank()) {

                JSONObject paymentJson =
                        razorpayPaymentService.fetchPayment(tenantId, order.getRazorpayPaymentId());

                applyPaymentStatus(order, paymentJson);
                orderRepository.save(order);
                return ResponseEntity.ok(order);
            }

            // 3️⃣ If paymentId not stored → fetch by orderId
            if (order.getRazorpayOrderId() != null && !order.getRazorpayOrderId().isBlank()) {

                List<JSONObject> payments =
                        razorpayPaymentService.fetchPaymentsByOrderId(
                                tenantId,
                                order.getRazorpayOrderId()
                        );

                if (payments != null && !payments.isEmpty()) {

                    JSONObject latest = payments.stream()
                            .max(Comparator.comparing(p ->
                                    p.optLong("created_at", 0)))
                            .orElse(null);

                    if (latest != null) {

                        String paymentId = latest.optString("id", "");
                        if (!paymentId.isBlank()) {
                            order.setRazorpayPaymentId(paymentId);
                        }

                        applyPaymentStatus(order, latest);
                        orderRepository.save(order);
                    }
                }

                return ResponseEntity.ok(order);
            }

            return ResponseEntity.ok(order);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Sync failed: " + e.getMessage());
        }
    }

    private void applyPaymentStatus(Order order, JSONObject po) {

        String st = po.optString("status", "");

        if ("captured".equalsIgnoreCase(st)) {
            order.setStatus(OrderStatus.PAID);
            if (order.getPaidAt() == null) {
                order.setPaidAt(Instant.now());
            }
            order.setFailedAt(null);

        } else if ("failed".equalsIgnoreCase(st)) {
            order.setStatus(OrderStatus.FAILED);
            if (order.getFailedAt() == null) {
                order.setFailedAt(Instant.now());
            }

        } else if ("authorized".equalsIgnoreCase(st)
                || "created".equalsIgnoreCase(st)) {

            order.setStatus(OrderStatus.PAYMENT_SENT);
        }
    }
}
