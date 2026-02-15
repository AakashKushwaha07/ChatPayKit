package com.chatpaykit.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chatpaykit.entity.TenantSettings;

public interface TenantSettingsRepository extends JpaRepository<TenantSettings, UUID> {
}
