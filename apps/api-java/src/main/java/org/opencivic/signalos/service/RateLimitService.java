package org.opencivic.signalos.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimitService {

    private final Map<String, AtomicInteger> attempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;

    public boolean tryAcquire(String key) {
        AtomicInteger counter = attempts.computeIfAbsent(key, k -> new AtomicInteger(0));
        return counter.incrementAndGet() <= MAX_ATTEMPTS;
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    @Scheduled(fixedRate = 60000) // Reset every minute
    public void cleanup() {
        attempts.clear();
    }
}
