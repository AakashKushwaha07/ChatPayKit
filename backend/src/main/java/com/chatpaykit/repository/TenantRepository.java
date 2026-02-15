package com.chatpaykit.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.chatpaykit.entity.Tenant;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
}
