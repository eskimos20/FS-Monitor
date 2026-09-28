package com.fsmonitor.app.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory login throttling: after {@code maxAttempts} consecutive failures a key
 * (username + client IP) is locked for {@code lockoutMinutes} minutes.
 * A successful login clears the counter. Entries are evicted periodically so the
 * map stays bounded even under a brute-force spray against many usernames.
 */
@Component
public class LoginRateLimiter {

    private final int maxAttempts;
    private final Duration lockout;

    private static final class Attempts {
        int failures;
        Instant lockedUntil;
        Instant lastSeen;
    }

    private final ConcurrentMap<String, Attempts> attempts = new ConcurrentHashMap<>();

    public LoginRateLimiter(
            @Value("${fsmonitor.security.login.max-attempts:5}") int maxAttempts,
            @Value("${fsmonitor.security.login.lockout-minutes:15}") long lockoutMinutes) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.lockout = Duration.ofMinutes(Math.max(1, lockoutMinutes));
    }

    /** Per-IP limit is more generous: blocks credential spraying without locking out shared NAT users quickly. */
    public int ipMaxAttempts() {
        return maxAttempts * 4;
    }

    public boolean isBlocked(String key) {
        Attempts a = attempts.get(key);
        if (a == null || a.lockedUntil == null) {
            return false;
        }
        if (Instant.now().isBefore(a.lockedUntil)) {
            return true;
        }
        // Lock expired - drop the entry so the client gets a fresh quota
        attempts.remove(key, a);
        return false;
    }

    public boolean isBlocked(String userKey, String ipKey) {
        return isBlocked(userKey) || isBlocked(ipKey);
    }

    /** Seconds until the lock expires; 0 when not locked. */
    public long remainingLockSeconds(String key) {
        Attempts a = attempts.get(key);
        if (a == null || a.lockedUntil == null) {
            return 0;
        }
        long remaining = Duration.between(Instant.now(), a.lockedUntil).getSeconds();
        return Math.max(0, remaining);
    }

    public void recordFailure(String key, int limit) {
        attempts.compute(key, (k, existing) -> {
            Attempts a = existing != null ? existing : new Attempts();
            a.failures++;
            a.lastSeen = Instant.now();
            if (a.failures >= limit) {
                a.lockedUntil = Instant.now().plus(lockout);
            }
            return a;
        });
    }

    /** Records a failure against both the account key and the source-IP key. */
    public void recordFailure(String userKey, String ipKey) {
        recordFailure(userKey, maxAttempts);
        recordFailure(ipKey, ipMaxAttempts());
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    /** A successful login clears the account counter and the source-IP counter. */
    public void recordSuccess(String userKey, String ipKey) {
        attempts.remove(userKey);
        attempts.remove(ipKey);
    }

    /** Bounds memory: drop stale entries once the map grows past a few thousand keys. */
    @Scheduled(fixedDelay = 300_000)
    void evictStaleEntries() {
        if (attempts.size() < 5_000) {
            return;
        }
        Instant cutoff = Instant.now().minus(lockout.multipliedBy(2));
        Iterator<Map.Entry<String, Attempts>> it = attempts.entrySet().iterator();
        while (it.hasNext()) {
            Attempts a = it.next().getValue();
            if (a.lastSeen.isBefore(cutoff) && (a.lockedUntil == null || Instant.now().isAfter(a.lockedUntil))) {
                it.remove();
            }
        }
    }
}
