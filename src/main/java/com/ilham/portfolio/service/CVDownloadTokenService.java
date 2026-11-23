
package com.ilham.portfolio.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CVDownloadTokenService {

    private static final Duration TOKEN_LIFETIME =
            Duration.ofMinutes(5);

    private final ConcurrentHashMap<String, Instant> tokens =
            new ConcurrentHashMap<>();

    public String generateToken() {
        Instant now = Instant.now();

        // Remove expired tokens
        tokens.entrySet().removeIf(entry ->
                now.isAfter(entry.getValue())
        );

        String token = UUID.randomUUID().toString();

        tokens.put(token, now.plus(TOKEN_LIFETIME));

        return token;
    }

    public boolean consumeToken(String token) {
        Instant expiresAt = tokens.remove(token);

        return expiresAt != null &&
                Instant.now().isBefore(expiresAt);
    }
}
