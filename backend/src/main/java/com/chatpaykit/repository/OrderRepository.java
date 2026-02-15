package com.chatpaykit.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chatpaykit.entity.Order;
import com.chatpaykit.entity.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    // ✅ NEW: SaaS tenant isolation
    List<Order> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    // (Optional) keep if you still use status filters somewhere
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);
    Optional<Order> findByRazorpayPaymentId(String razorpayPaymentId);
}
