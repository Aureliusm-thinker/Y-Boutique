package com.yvette.api.staff;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Short-lived "password was correct, now enter your code" tickets for two-step staff sign-in.
 * In-memory (per API instance), like {@link LoginRateLimiter}; move to Redis when scaling out.
 */
@Component
public class MfaChallenges {

    static final Duration TTL = Duration.ofMinutes(5);
    static final int MAX_ATTEMPTS = 5;

    private record Challenge(Long staffId, Instant expiresAt, int attempts) {}

    private final Map<String, Challenge> challenges = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String create(Long staffId) {
        challenges.values().removeIf(c -> c.expiresAt().isBefore(Instant.now()));
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        challenges.put(ticket, new Challenge(staffId, Instant.now().plus(TTL), 0));
        return ticket;
    }

    /** The staff id for a live ticket, or null if it is unknown, expired or used up. */
    public Long staffIdFor(String ticket) {
        Challenge c = ticket == null ? null : challenges.get(ticket);
        if (c == null) return null;
        if (c.expiresAt().isBefore(Instant.now()) || c.attempts() >= MAX_ATTEMPTS) {
            challenges.remove(ticket);
            return null;
        }
        return c.staffId();
    }

    public void recordFailure(String ticket) {
        challenges.computeIfPresent(ticket,
                (k, c) -> new Challenge(c.staffId(), c.expiresAt(), c.attempts() + 1));
    }

    public void consume(String ticket) {
        challenges.remove(ticket);
    }
}
