package com.testplatform.config;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory token store for admin auth.
 * Good enough for a single-instance basic test platform; not meant for
 * multi-instance / production-grade deployments.
 */
@Component
public class AdminTokenStore {

    private static final long TOKEN_TTL_SECONDS = 12 * 60 * 60; // 12 hours

    private final Map<String, Instant> tokens = new ConcurrentHashMap<>();

    public String issueToken() {
        String token = UUID.randomUUID().toString();
        tokens.put(token, Instant.now());
        return token;
    }

    public boolean isValid(String token) {
        if (token == null) return false;
        Instant issuedAt = tokens.get(token);
        if (issuedAt == null) return false;
        if (Instant.now().isAfter(issuedAt.plusSeconds(TOKEN_TTL_SECONDS))) {
            tokens.remove(token);
            return false;
        }
        return true;
    }
}
