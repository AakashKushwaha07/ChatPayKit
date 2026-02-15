package com.chatpaykit.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chatpaykit.entity.AppSettings;

public interface AppSettingsRepository extends JpaRepository<AppSettings, Long> {}
