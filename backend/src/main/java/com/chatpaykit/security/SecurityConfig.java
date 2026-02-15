package com.chatpaykit.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            // ✅ public auth endpoints
            .requestMatchers("/api/auth/**").permitAll()

            // ✅ H2 console
            .requestMatchers("/h2-console/**").permitAll()

            // ✅ checkout page must be public (static)
            .requestMatchers(HttpMethod.GET, "/checkout.html").permitAll()
            .requestMatchers(HttpMethod.GET, "/favicon.ico").permitAll()
            .requestMatchers(HttpMethod.GET, "/static/**").permitAll()

            // ✅ allow OPTIONS preflight (CORS)
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

            // ✅ everything else needs auth
            .anyRequest().authenticated()
        )
        .headers(h -> h.frameOptions(f -> f.disable()))
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}

}
