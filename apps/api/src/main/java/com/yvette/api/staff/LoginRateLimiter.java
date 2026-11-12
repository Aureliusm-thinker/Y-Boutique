package com.yvette.api.staff;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Blocks brute-force login attempts: max 5 failures per email per 15 minutes (NFR-04).
 * In-memory, so per API instance; move to Redis when we run more than one instance.
 */
@Component
public class LoginRateLimiter {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private record Failures(int count, Instant windowStart) {}

    private final Map<String, Failures> failures = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        Failures f = failures.get(key);
        if (f == null) return false;
        if (f.windowStart().plus(WINDOW).isBefore(Instant.now())) {
            failures.remove(key);
            return false;
        }
        return f.count() >= MAX_FAILURES;
    }

    public void recordFailure(String key) {
        Instant now = Instant.now();
        failures.compute(key, (k, f) -> (f == null || f.windowStart().plus(WINDOW).isBefore(now))
                ? new Failures(1, now)
                : new Failures(f.count() + 1, f.windowStart()));
    }

    public void reset(String key) {
        failures.remove(key);
    }
}
