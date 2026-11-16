package com.edusphere.security;

import com.edusphere.common.ApiException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Small in-memory sliding-window limiter for guessable inputs (login, reset requests, ...). */
@Component
public class LoginRateLimiter {
    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private final ConcurrentHashMap<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public void checkAllowed(String key) {
        Deque<Instant> d = failures.get(key);
        if (d == null) return;
        synchronized (d) {
            prune(d);
            if (d.size() >= MAX_FAILURES) {
                throw ApiException.tooMany("Too many attempts. Please wait a few minutes and try again.");
            }
        }
    }

    public void recordFailure(String key) {
        Deque<Instant> d = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (d) {
            prune(d);
            d.addLast(Instant.now());
        }
    }

    public void reset(String key) { failures.remove(key); }

    private void prune(Deque<Instant> d) {
        Instant cutoff = Instant.now().minus(WINDOW);
        while (!d.isEmpty() && d.peekFirst().isBefore(cutoff)) d.pollFirst();
    }
}
