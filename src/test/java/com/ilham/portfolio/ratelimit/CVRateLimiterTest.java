
package com.ilham.portfolio.ratelimit;

import com.ilham.portfolio.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class CVRateLimiterTest {

    private CVRateLimiter rateLimiter;
    private MutableClock clock;

    private static final String VISITOR_IP = "192.168.1.10";

    @BeforeEach
    void setUp() {
        clock = new MutableClock(
                Instant.parse("2026-01-01T10:00:00Z"),
                ZoneOffset.UTC
        );

        rateLimiter = new CVRateLimiter(clock);
    }

    @Test
    void shouldAllowFirstThreeRequests() {

        assertDoesNotThrow(() -> {
            rateLimiter.checkLimit(VISITOR_IP);
            rateLimiter.checkLimit(VISITOR_IP);
            rateLimiter.checkLimit(VISITOR_IP);
        });
    }

    @Test
    void shouldRejectFourthRequest() {

        rateLimiter.checkLimit(VISITOR_IP);
        rateLimiter.checkLimit(VISITOR_IP);
        rateLimiter.checkLimit(VISITOR_IP);

        assertThrows(
                RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(VISITOR_IP)
        );
    }

    @Test
    void shouldRejectRequestBeforeWindowExpires() {

        rateLimiter.checkLimit(VISITOR_IP);
        rateLimiter.checkLimit(VISITOR_IP);
        rateLimiter.checkLimit(VISITOR_IP);

        clock.advance(Duration.ofMinutes(9));

        assertThrows(
                RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(VISITOR_IP)
        );
    }

    @Test
    void shouldAllowRequestsAfterWindowExpires() {

        rateLimiter.checkLimit(VISITOR_IP);
        rateLimiter.checkLimit(VISITOR_IP);
        rateLimiter.checkLimit(VISITOR_IP);

        clock.advance(Duration.ofMinutes(10));

        assertDoesNotThrow(() -> {
            rateLimiter.checkLimit(VISITOR_IP);
            rateLimiter.checkLimit(VISITOR_IP);
            rateLimiter.checkLimit(VISITOR_IP);
        });

        assertThrows(
                RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(VISITOR_IP)
        );
    }

    @Test
    void shouldMaintainSeparateLimitsForDifferentVisitors() {

        String secondVisitor = "192.168.1.20";

        rateLimiter.checkLimit(VISITOR_IP);
        rateLimiter.checkLimit(VISITOR_IP);
        rateLimiter.checkLimit(VISITOR_IP);

        assertThrows(
                RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(VISITOR_IP)
        );

        assertDoesNotThrow(
                () -> rateLimiter.checkLimit(secondVisitor)
        );
    }

    @Test
    void shouldAllowRequestsAfterExpiredWindowCleanup() {

        rateLimiter.checkLimit(VISITOR_IP);
        rateLimiter.checkLimit(VISITOR_IP);
        rateLimiter.checkLimit(VISITOR_IP);

        clock.advance(Duration.ofMinutes(10));

        rateLimiter.removeExpiredWindows();

        assertDoesNotThrow(
                () -> rateLimiter.checkLimit(VISITOR_IP)
        );
    }

    // Test clock that allows us to simulate time passing
    private static class MutableClock extends Clock {

        private Instant instant;
        private final ZoneId zone;

        MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
