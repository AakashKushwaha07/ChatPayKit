package com.chatpaykit.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.chatpaykit.dto.CreateOrderRequest;
import com.chatpaykit.dto.OrderResponse;
import com.chatpaykit.dto.OrderStatusResponse;
import com.chatpaykit.dto.VerifyPaymentRequest;
import com.chatpaykit.entity.Order;
import com.chatpaykit.service.AppSettingsService;
import com.chatpaykit.service.JwtService;
import com.chatpaykit.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final AppSettingsService appSettingsService;
    private final JwtService jwtService;

    // -------------------------
    // Create (tenant-aware)
    // -------------------------
    @PostMapping
    public OrderResponse create(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreateOrderRequest req
    ) {
        UUID tenantId = tenantIdFrom(authHeader);
        return OrderResponse.from(orderService.create(req, tenantId));
    }

    // -------------------------
    // List (tenant-aware) ✅ FIXED
    // -------------------------
    @GetMapping
    public List<OrderResponse> list(@RequestHeader("Authorization") String authHeader) {
        UUID tenantId = tenantIdFrom(authHeader);

        return orderService.listByTenant(tenantId)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    // -------------------------
    // Verify (checkout signature)
    // -------------------------
    @PostMapping("/{id}/verify")
    public OrderResponse verify(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id,
            @Valid @RequestBody VerifyPaymentRequest req
    ) {
        UUID tenantId = tenantIdFrom(authHeader);
        assertOrderBelongsToTenant(id, tenantId);

        return OrderResponse.from(orderService.verifyPayment(id, req));
    }

    // -------------------------
    // Get single order (tenant-safe)
    // -------------------------
    @GetMapping("/{id}")
    public OrderResponse get(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id
    ) {
        UUID tenantId = tenantIdFrom(authHeader);
        Order order = assertOrderBelongsToTenant(id, tenantId);

        return OrderResponse.from(order);
    }

    // -------------------------
    // Send WhatsApp payment request (tenant-safe)
    // -------------------------
    @PostMapping("/{id}/send-payment")
    public OrderResponse sendPayment(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id
    ) {
        UUID tenantId = tenantIdFrom(authHeader);
        assertOrderBelongsToTenant(id, tenantId);

        return OrderResponse.from(orderService.sendPaymentRequest(id));
    }

    // -------------------------
    // Status API (tenant-safe)
    // -------------------------
    @GetMapping("/{id}/status")
    public OrderStatusResponse status(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id
    ) {
        UUID tenantId = tenantIdFrom(authHeader);
        Order order = assertOrderBelongsToTenant(id, tenantId);

        return OrderStatusResponse.from(order);
    }

    // -------------------------
    // Retry payment (tenant-safe)
    // -------------------------
    @PostMapping("/{id}/retry")
    public OrderResponse retry(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id
    ) {
        UUID tenantId = tenantIdFrom(authHeader);
        assertOrderBelongsToTenant(id, tenantId);

        return OrderResponse.from(orderService.retry(id));
    }

    // -------------------------
    // Refund (tenant-safe)
    // -------------------------
    @PostMapping("/{id}/refund")
    public OrderResponse refund(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id
    ) {
        UUID tenantId = tenantIdFrom(authHeader);
        assertOrderBelongsToTenant(id, tenantId);

        return OrderResponse.from(orderService.refund(id));
    }

    // -------------------------
    // Checkout URL builder (tenant-aware + auth-protected)
    // -------------------------
    @GetMapping("/{id}/checkout")
    public Map<String, String> checkout(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id
    ) {
        UUID tenantId = tenantIdFrom(authHeader);
        Order order = assertOrderBelongsToTenant(id, tenantId);

        if (order.getRazorpayOrderId() == null || order.getRazorpayOrderId().isBlank()) {
            throw new IllegalStateException(
                    "Call /api/orders/{id}/send-payment first to create Razorpay order"
            );
        }

        // ✅ KeyId from Tenant Settings (multi-tenant)
        var settings = appSettingsService.getOrThrow(order.getTenantId());
        String razorpayKeyId = settings.getRazorpayKeyId();

        if (razorpayKeyId == null || razorpayKeyId.isBlank()) {
            throw new IllegalStateException(
                    "Razorpay keyId missing (Tenant settings not configured)"
            );
        }

        String desc = URLEncoder.encode("Order " + order.getId(), StandardCharsets.UTF_8);

        String url = "http://localhost:8080/checkout.html"
                + "?dbOrderId=" + order.getId()
                + "&orderId=" + order.getRazorpayOrderId()
                + "&keyId=" + razorpayKeyId
                + "&amount=" + order.getAmountPaise()
                + "&name=ChatPayKit"
                + "&desc=" + desc;

        return Map.of("url", url);
    }

    // -----------------------------
    // Helpers
    // -----------------------------
    private UUID tenantIdFrom(String authHeader) {
        String token = extractToken(authHeader);
        return jwtService.extractTenantId(token);
    }

    private Order assertOrderBelongsToTenant(UUID orderId, UUID tenantId) {
        Order order = orderService.get(orderId);

        if (order.getTenantId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Order has no tenantId. Fix existing rows in DB.");
        }
        if (!tenantId.equals(order.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to access this order.");
        }
        return order;
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalStateException("Missing or invalid Authorization header");
        }
        return authHeader.substring(7).trim();
    }
}
