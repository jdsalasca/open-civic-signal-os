package org.opencivic.signalos.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final Map<String, RateWindow> attempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;

    public boolean tryAcquire(String key) {
        return tryAcquire(key, MAX_ATTEMPTS, Duration.ofMinutes(1)).allowed();
    }

    public RateLimitDecision tryAcquire(String key, int maxAttempts, Duration duration) {
        long now = Instant.now().getEpochSecond();
        RateWindow window = attempts.compute(key, (ignored, current) -> {
            if (current == null || current.resetAtEpochSecond() <= now) {
                return new RateWindow(1, now + duration.getSeconds());
            }
            return new RateWindow(current.count() + 1, current.resetAtEpochSecond());
        });
        boolean allowed = window.count() <= maxAttempts;
        int remaining = Math.max(0, maxAttempts - Math.min(window.count(), maxAttempts));
        return new RateLimitDecision(allowed, maxAttempts, remaining, window.resetAtEpochSecond());
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    @Scheduled(fixedRate = 60000) // Reset every minute
    public void cleanup() {
        long now = Instant.now().getEpochSecond();
        attempts.entrySet().removeIf(entry -> entry.getValue().resetAtEpochSecond() <= now);
    }

    private record RateWindow(int count, long resetAtEpochSecond) {}

    public record RateLimitDecision(boolean allowed, int limit, int remaining, long resetAtEpochSecond) {}
}
