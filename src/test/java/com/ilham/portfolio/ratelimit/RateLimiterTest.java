
package com.ilham.portfolio.ratelimit;

import com.ilham.portfolio.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RateLimiterTest {

    @Test
    void shouldAllowTenRequestsAndRejectEleventh() {

        RateLimiter rateLimiter = new RateLimiter();

        String visitorId = "visitor-1";

        // The first 10 requests should succeed
        for (int i = 0; i < 10; i++) {
            assertDoesNotThrow(() ->
                    rateLimiter.checkLimit(visitorId)
            );
        }

        // The 11th request should be rejected
        assertThrows(
                RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(visitorId)
        );
    }

    @Test
    void shouldTrackVisitorsSeparately() {

        RateLimiter rateLimiter = new RateLimiter();

        // Visitor 1 reaches the limit
        for (int i = 0; i < 10; i++) {
            rateLimiter.checkLimit("visitor-1");
        }

        // Visitor 2 should still be allowed
        assertDoesNotThrow(() ->
                rateLimiter.checkLimit("visitor-2")
        );

        // Visitor 1 should be blocked
        assertThrows(
                RateLimitExceededException.class,
                () -> rateLimiter.checkLimit("visitor-1")
        );
    }



    @Test
    void shouldResetRateLimitAfterOneMinute() {

        Instant start = Instant.parse("2026-01-01T10:00:00Z");

        AtomicReference<Instant> currentTime =
                new AtomicReference<>(start);

        Clock clock = new Clock() {

            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return Clock.fixed(currentTime.get(), zone);
            }

            @Override
            public Instant instant() {
                return currentTime.get();
            }
        };

        RateLimiter rateLimiter = new RateLimiter(clock);

        String visitorId = "visitor-1";

        // Use all 10 allowed requests
        for (int i = 0; i < 10; i++) {
            rateLimiter.checkLimit(visitorId);
        }

        // The 11th request must be rejected
        assertThrows(
                RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(visitorId)
        );

        // Advance time by 61 seconds
        currentTime.set(start.plus(Duration.ofSeconds(61)));

        // A new request should now be allowed
        assertDoesNotThrow(
                () -> rateLimiter.checkLimit(visitorId)
        );
    }



    @Test
    void shouldAllowNewRequestsAfterExpiredWindowCleanup() {

        Instant start = Instant.parse("2026-01-01T10:00:00Z");

        AtomicReference<Instant> currentTime =
                new AtomicReference<>(start);

        Clock clock = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return Clock.fixed(currentTime.get(), zone);
            }

            @Override
            public Instant instant() {
                return currentTime.get();
            }
        };

        RateLimiter rateLimiter = new RateLimiter(clock);

        for (int i = 0; i < 10; i++) {
            rateLimiter.checkLimit("visitor-1");
        }

        assertThrows(
                RateLimitExceededException.class,
                () -> rateLimiter.checkLimit("visitor-1")
        );

        currentTime.set(start.plus(Duration.ofSeconds(61)));

        rateLimiter.removeExpiredWindows();

        assertDoesNotThrow(
                () -> rateLimiter.checkLimit("visitor-1")
        );
    }



}
