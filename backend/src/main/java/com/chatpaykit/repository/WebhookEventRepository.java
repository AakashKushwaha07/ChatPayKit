package com.chatpaykit.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chatpaykit.entity.WebhookEvent;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, String> {
}
