package com.testplatform.config;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory token store for teacher auth.
 */
@Component
public class AdminTokenStore {

    private static final long TOKEN_TTL_SECONDS = 12 * 60 * 60; // 12 hours

    public static class TokenDetails {
        private final String email;
        private final Instant issuedAt;

        public TokenDetails(String email, Instant issuedAt) {
            this.email = email;
            this.issuedAt = issuedAt;
        }

        public String getEmail() {
            return email;
        }

        public Instant getIssuedAt() {
            return issuedAt;
        }
    }

    private final Map<String, TokenDetails> tokens = new ConcurrentHashMap<>();

    public String issueToken(String email) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, new TokenDetails(email, Instant.now()));
        return token;
    }

    public boolean isValid(String token) {
        if (token == null) return false;
        TokenDetails details = tokens.get(token);
        if (details == null) return false;
        if (Instant.now().isAfter(details.getIssuedAt().plusSeconds(TOKEN_TTL_SECONDS))) {
            tokens.remove(token);
            return false;
        }
        return true;
    }

    public String getEmail(String token) {
        TokenDetails details = tokens.get(token);
        return details != null ? details.getEmail() : null;
    }
}
