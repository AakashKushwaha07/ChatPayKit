package com.chatpaykit.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chatpaykit.dto.AuthLoginRequest;
import com.chatpaykit.dto.AuthSignupRequest;
import com.chatpaykit.entity.Tenant;
import com.chatpaykit.entity.User;
import com.chatpaykit.repository.TenantRepository;
import com.chatpaykit.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Transactional
    public String signup(AuthSignupRequest req) {

        // email unique
        userRepository.findByEmail(req.getEmail().trim().toLowerCase())
                .ifPresent(u -> { throw new IllegalStateException("Email already registered"); });

        Tenant tenant = Tenant.builder()
                .name(req.getTenantName().trim())
                .build();
        tenant = tenantRepository.save(tenant);

        User user = User.builder()
                .tenantId(tenant.getId())
                .email(req.getEmail().trim().toLowerCase())
                .passwordHash(encoder.encode(req.getPassword()))
                .role("ADMIN")
                .build();
        user = userRepository.save(user);

        return jwtService.generateToken(user.getId(), tenant.getId(), user.getRole());
    }

    @Transactional(readOnly = true)
    public String login(AuthLoginRequest req) {

        User user = userRepository.findByEmail(req.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new IllegalStateException("Invalid email or password"));

        if (!encoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new IllegalStateException("Invalid email or password");
        }

        return jwtService.generateToken(user.getId(), user.getTenantId(), user.getRole());
    }
}
