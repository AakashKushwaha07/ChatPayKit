package com.chatpaykit.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chatpaykit.dto.CreateOrderRequest;
import com.chatpaykit.dto.VerifyPaymentRequest;
import com.chatpaykit.entity.Order;
import com.chatpaykit.entity.OrderStatus;
import com.chatpaykit.exception.ApiException;
import com.chatpaykit.repository.OrderRepository;
import com.chatpaykit.util.RazorpayCheckoutSignatureUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final RazorpayPaymentService razorpayPaymentService;
    private final AppSettingsService appSettingsService;

    // -------------------------
    // Create / Read / List
    // -------------------------

    @Transactional
    public Order create(CreateOrderRequest req, UUID tenantId) {

        Order order = Order.builder()
                .tenantId(tenantId) // ✅ IMPORTANT (Step 1)
                .customerName(req.getCustomerName().trim())
                .customerWhatsapp(req.getCustomerWhatsapp().trim())
                .amountPaise(req.getAmountPaise())
                .currency("INR")
                .description(req.getDescription())
                .status(OrderStatus.CREATED)
                .attemptCount(0)
                .build();

        Order saved = orderRepository.save(order);

        log.info("Order created id={} tenantId={} amountPaise={} whatsapp={}",
                saved.getId(), saved.getTenantId(), saved.getAmountPaise(), maskWhatsapp(saved.getCustomerWhatsapp()));

        return saved;
    }

    @Transactional(readOnly = true)
    public Order get(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Order> listAll() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public OrderStatus getStatus(UUID id) {
        return get(id).getStatus();
    }

    // -------------------------
    // Send Payment Request
    // -------------------------

    @Transactional
    public Order sendPaymentRequest(UUID id) {
        Order order = get(id);

        if (order.getStatus() != null && order.getStatus().isTerminal()) {
            throw new ApiException(HttpStatus.CONFLICT, "Order is terminal: " + order.getStatus());
        }

        // ✅ Create Razorpay order + send WhatsApp request (single source of truth)
        String rzpOrderId = razorpayPaymentService.sendPaymentRequestOnWhatsapp(order);

        order.setRazorpayOrderId(rzpOrderId);
        order.setWhatsappPaymentReferenceId("pay_" + order.getId());

        safeTransition(order, OrderStatus.PAYMENT_SENT, "sendPaymentRequest");

        log.info("Payment request sent id={} razorpayOrderId={}",
                order.getId(), order.getRazorpayOrderId());

        return orderRepository.save(order);
    }

    // -------------------------
    // Verify Payment (checkout signature)
    // -------------------------

    @Transactional
    public Order verifyPayment(UUID id, VerifyPaymentRequest req) {

        Order order = get(id);

        // ✅ Get Razorpay keySecret from Admin Settings
        var settings = appSettingsService.getOrThrow(order.getTenantId());

        String razorpayKeySecret = settings.getRazorpayKeySecret();

        if (razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Razorpay keySecret missing (Admin settings not configured)");
        }

        // ✅ Idempotency: if already PAID, just return (fill paymentId once)
        if (order.getStatus() == OrderStatus.PAID) {
            if ((order.getRazorpayPaymentId() == null || order.getRazorpayPaymentId().isBlank())
                    && req.getRazorpayPaymentId() != null
                    && !req.getRazorpayPaymentId().isBlank()) {

                order.setRazorpayPaymentId(req.getRazorpayPaymentId());
                if (order.getPaidAt() == null) order.setPaidAt(Instant.now());
                if (order.getVerifiedAt() == null) order.setVerifiedAt(Instant.now());

                return orderRepository.save(order);
            }
            return order;
        }

        if (order.getRazorpayOrderId() == null || order.getRazorpayOrderId().isBlank()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Order has no Razorpay orderId. Call /send-payment first.");
        }

        if (!order.getRazorpayOrderId().equals(req.getRazorpayOrderId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OrderId mismatch");
        }

        boolean ok = RazorpayCheckoutSignatureUtil.verify(
                req.getRazorpayOrderId(),
                req.getRazorpayPaymentId(),
                req.getRazorpaySignature(),
                razorpayKeySecret
        );

        if (!ok) {
            order.setLastError("Invalid checkout signature");
            orderRepository.save(order);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid checkout signature");
        }

        order.setRazorpayPaymentId(req.getRazorpayPaymentId());
        order.setVerifiedAt(Instant.now());

        safeTransition(order, OrderStatus.PAID, "verifyPayment");

        if (order.getPaidAt() == null) order.setPaidAt(Instant.now());
        order.setFailedAt(null);
        order.setLastError(null);

        log.info("Payment verified id={} paymentId={}",
                order.getId(), order.getRazorpayPaymentId());

        return orderRepository.save(order);
    }

    // -------------------------
    // Retry (ONLY if FAILED)
    // -------------------------

    @Transactional
    public Order retry(UUID id) {
        Order order = get(id);

        if (order.getStatus() != OrderStatus.FAILED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Retry allowed only when status is FAILED.");
        }
        if (order.getStatus() != null && order.getStatus().isTerminal()) {
            throw new ApiException(HttpStatus.CONFLICT, "Order is terminal: " + order.getStatus());
        }

        Integer attempts = order.getAttemptCount();
        order.setAttemptCount(attempts == null ? 1 : attempts + 1);

        order.setFailedAt(null);
        order.setLastError(null);
        order.setRazorpayPaymentId(null);

        String newRzpOrderId = razorpayPaymentService.sendRetryPaymentRequestOnWhatsapp(order);
        order.setRazorpayOrderId(newRzpOrderId);
        order.setWhatsappPaymentReferenceId("pay_" + order.getId());

        safeTransition(order, OrderStatus.PAYMENT_SENT, "retry");

        log.info("Retry initiated id={} attemptCount={} newRzpOrderId={}",
                order.getId(), order.getAttemptCount(), order.getRazorpayOrderId());

        return orderRepository.save(order);
    }
@Transactional(readOnly = true)
public List<Order> listByTenant(UUID tenantId) {
    return orderRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
}

    // -------------------------
    // Refund (ONLY if PAID)
    // -------------------------

    @Transactional
    public Order refund(UUID id) {
        Order order = get(id);

        if (order.getStatus() != OrderStatus.PAID) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Refund allowed only when status is PAID.");
        }
        if (order.getRazorpayPaymentId() == null || order.getRazorpayPaymentId().isBlank()) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot refund: razorpayPaymentId is missing.");
        }

        safeTransition(order, OrderStatus.REFUND_PENDING, "refund");
        orderRepository.save(order);

        try {
            String refundId = razorpayPaymentService.refundPayment(order.getTenantId(), order.getRazorpayPaymentId());
            order.setRazorpayRefundId(refundId);

            order.setRefundedAt(Instant.now());

            log.info("Refund requested id={} paymentId={} refundId={}",
                    order.getId(), order.getRazorpayPaymentId(), refundId);

            return orderRepository.save(order);

        } catch (Exception e) {
            order.setLastError("Refund failed: " + e.getMessage());
            log.error("Refund failed id={} msg={}", order.getId(), e.getMessage(), e);
            orderRepository.save(order);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Refund failed. Check server logs.");
        }
    }

    // -------------------------
    // Helpers
    // -------------------------

    private void safeTransition(Order order, OrderStatus next, String reason) {
        OrderStatus current = order.getStatus();
        if (current == null) {
            order.setStatus(next);
            return;
        }
        if (!current.canTransitionTo(next)) {
            log.warn("Blocked invalid/downgrade transition id={} {} -> {} reason={}",
                    order.getId(), current, next, reason);
            return;
        }
        order.setStatus(next);
    }

    private String maskWhatsapp(String s) {
        if (s == null) return null;
        if (s.length() <= 4) return "****";
        return "****" + s.substring(s.length() - 4);
    }
}
