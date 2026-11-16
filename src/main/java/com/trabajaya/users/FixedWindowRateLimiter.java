package com.trabajaya.users;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Single-process limiter with bounded storage and a testable clock. */
public final class FixedWindowRateLimiter {
    private record Window(long start, int count) { }

    private final Map<String, Window> windows = new HashMap<>();
    private final Clock clock;
    private final long windowMillis;
    private final int capacity;

    public FixedWindowRateLimiter(Clock clock, Duration window, int capacity) {
        this.clock = Objects.requireNonNull(clock);
        this.windowMillis = window.toMillis();
        this.capacity = capacity;
        if (windowMillis < 1 || capacity < 1) {
            throw new IllegalArgumentException("Window and capacity must be positive");
        }
    }

    public synchronized boolean allow(String key, int limit) {
        if (key == null || limit < 1) throw new IllegalArgumentException("Invalid limit/key");
        long now = clock.millis();
        Window previous = windows.get(key);
        if (previous != null && now - previous.start() >= windowMillis) previous = null;
        if (previous == null) {
            if (windows.size() >= capacity) {
                windows.entrySet().removeIf(e -> now - e.getValue().start() >= windowMillis);
            }
            if (!windows.containsKey(key) && windows.size() >= capacity) return false;
            windows.put(key, new Window(now, 1));
            return true;
        }
        if (previous.count() >= limit) return false;
        windows.put(key, new Window(previous.start(), previous.count() + 1));
        return true;
    }

    public synchronized int size() { return windows.size(); }
}
