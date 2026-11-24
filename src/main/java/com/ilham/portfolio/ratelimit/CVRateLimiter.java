package com.ilham.portfolio.ratelimit;

import com.ilham.portfolio.exception.RateLimitExceededException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CVRateLimiter {

    private static final int MAX_REQUESTS = 3;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private final Clock clock;

    private final Map<String, RequestWindow> requests =
            new ConcurrentHashMap<>();

    public CVRateLimiter() {
        this(Clock.systemUTC());
    }

    CVRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public void checkLimit(String visitorId) {

        requests.compute(visitorId, (key, window) -> {

            Instant now = Instant.now(clock);

            if (window == null ||
                    !now.isBefore(window.start().plus(WINDOW))) {

                return new RequestWindow(now, 1);
            }

            if (window.count() >= MAX_REQUESTS) {
                throw new RateLimitExceededException(
                        "Too many CV download attempts. " +
                                "Please try again later."
                );
            }

            return new RequestWindow(
                    window.start(),
                    window.count() + 1
            );
        });
    }

    @Scheduled(fixedDelay = 60_000)
    public void removeExpiredWindows() {

        Instant now = Instant.now(clock);

        requests.forEach((visitorId, window) ->
                requests.computeIfPresent(visitorId, (key, current) -> {

                    if (!now.isBefore(current.start().plus(WINDOW))) {
                        return null;
                    }

                    return current;
                })
        );
    }

    private record RequestWindow(Instant start, int count) {
    }
}

