package com.example.escort.module.auth.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginRateLimiterTest {

    @Test
    void lockAfterThresholdAndRecoverAfterLockExpires() {
        MutableClock clock = new MutableClock(1_000_000L);
        LoginRateLimiter limiter = new LoginRateLimiter(2, 5, 1, clock);

        limiter.recordFailure("127.0.0.1", "u1");
        assertEquals(0, limiter.checkLocked("127.0.0.1", "u1"));
        limiter.recordFailure("127.0.0.1", "u1");
        assertTrue(limiter.checkLocked("127.0.0.1", "u1") > 0);

        clock.advanceMinutes(1);
        assertEquals(0, limiter.checkLocked("127.0.0.1", "u1"));
    }

    @Test
    void successResetClearsCounter() {
        MutableClock clock = new MutableClock(2_000_000L);
        LoginRateLimiter limiter = new LoginRateLimiter(3, 5, 5, clock);

        limiter.recordFailure("10.0.0.1", "u2");
        limiter.recordFailure("10.0.0.1", "u2");
        limiter.reset("10.0.0.1", "u2");
        limiter.recordFailure("10.0.0.1", "u2");
        assertEquals(0, limiter.checkLocked("10.0.0.1", "u2"));
    }

    @Test
    void windowExpiryDropsOldFailures() {
        MutableClock clock = new MutableClock(3_000_000L);
        LoginRateLimiter limiter = new LoginRateLimiter(3, 1, 5, clock);

        limiter.recordFailure("10.0.0.2", "u3");
        limiter.recordFailure("10.0.0.2", "u3");
        clock.advanceMinutes(2);
        limiter.recordFailure("10.0.0.2", "u3");
        assertEquals(0, limiter.checkLocked("10.0.0.2", "u3"));
        limiter.recordFailure("10.0.0.2", "u3");
        limiter.recordFailure("10.0.0.2", "u3");
        assertTrue(limiter.checkLocked("10.0.0.2", "u3") > 0);
    }

    @Test
    void cleanupRemoveExpiredBuckets() {
        MutableClock clock = new MutableClock(4_000_000L);
        LoginRateLimiter limiter = new LoginRateLimiter(3, 1, 1, clock);
        limiter.recordFailure("10.0.0.3", "u4");
        clock.advanceMinutes(5);
        limiter.cleanup();
        assertEquals(0, limiter.checkLocked("10.0.0.3", "u4"));
    }

    private static class MutableClock extends Clock {
        private long millis;

        private MutableClock(long millis) {
            this.millis = millis;
        }

        void advanceMinutes(long minutes) {
            millis += minutes * 60_000L;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis);
        }

        @Override
        public long millis() {
            return millis;
        }
    }
}
