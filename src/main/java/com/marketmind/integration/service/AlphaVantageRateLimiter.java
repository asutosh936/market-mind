package com.marketmind.integration.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter for Alpha Vantage API calls.
 * Free tier allows 5 requests per minute.
 * This implementation tracks requests per minute and blocks when limit exceeded.
 */
@Component
public class AlphaVantageRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(AlphaVantageRateLimiter.class);

    // Free tier limits
    private static final int MAX_REQUESTS_PER_MINUTE = 5;
    private static final int TIME_WINDOW_MINUTES = 1;

    // Thread-safe storage for request tracking
    private final ConcurrentHashMap<String, RequestWindow> requestWindows = new ConcurrentHashMap<>();

    /**
     * Check if a request can be made for the given key.
     * @param key Unique identifier for the rate limit window (e.g., "global" for all requests)
     * @return true if request is allowed, false if rate limited
     */
    public boolean canMakeRequest(String key) {
        RequestWindow window = requestWindows.computeIfAbsent(key, k -> new RequestWindow());

        synchronized (window) {
            LocalDateTime now = LocalDateTime.now();

            // Reset window if time window has passed
            if (ChronoUnit.MINUTES.between(window.windowStart, now) >= TIME_WINDOW_MINUTES) {
                window.reset(now);
            }

            // Check if under limit
            if (window.requestCount.get() < MAX_REQUESTS_PER_MINUTE) {
                window.requestCount.incrementAndGet();
                logger.debug("Request allowed for key={}, count={}/{}", key, window.requestCount.get(), MAX_REQUESTS_PER_MINUTE);
                return true;
            } else {
                long secondsUntilReset = TIME_WINDOW_MINUTES * 60 - ChronoUnit.SECONDS.between(window.windowStart, now);
                logger.warn("Rate limit exceeded for key={}, count={}/{}, reset in {} seconds",
                           key, window.requestCount.get(), MAX_REQUESTS_PER_MINUTE, secondsUntilReset);
                return false;
            }
        }
    }

    /**
     * Get remaining requests in current window for the given key.
     */
    public int getRemainingRequests(String key) {
        RequestWindow window = requestWindows.get(key);
        if (window == null) {
            return MAX_REQUESTS_PER_MINUTE;
        }

        synchronized (window) {
            LocalDateTime now = LocalDateTime.now();

            // Reset window if time window has passed
            if (ChronoUnit.MINUTES.between(window.windowStart, now) >= TIME_WINDOW_MINUTES) {
                window.reset(now);
                return MAX_REQUESTS_PER_MINUTE;
            }

            return Math.max(0, MAX_REQUESTS_PER_MINUTE - window.requestCount.get());
        }
    }

    /**
     * Get seconds until rate limit window resets for the given key.
     */
    public long getSecondsUntilReset(String key) {
        RequestWindow window = requestWindows.get(key);
        if (window == null) {
            return 0;
        }

        synchronized (window) {
            LocalDateTime now = LocalDateTime.now();
            long secondsElapsed = ChronoUnit.SECONDS.between(window.windowStart, now);
            long windowSeconds = TIME_WINDOW_MINUTES * 60;
            return Math.max(0, windowSeconds - secondsElapsed);
        }
    }

    /**
     * Inner class to track request windows.
     */
    private static class RequestWindow {
        private LocalDateTime windowStart;
        private AtomicInteger requestCount;

        public RequestWindow() {
            reset(LocalDateTime.now());
        }

        public void reset(LocalDateTime now) {
            this.windowStart = now;
            this.requestCount = new AtomicInteger(0);
        }
    }
}