package com.trabajaya;

import com.trabajaya.events.DomainEvent;
import com.trabajaya.shared.PageWindow;
import com.trabajaya.storage.ObjectKey;
import com.trabajaya.users.FixedWindowRateLimiter;
import com.trabajaya.users.PasswordPolicy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BackendRulesTest {
    @Test void passwordPolicyHonorsBcryptByteLimit() {
        assertTrue(PasswordPolicy.valid("Prueba2026!"));
        assertFalse(PasswordPolicy.valid("password"));
        assertFalse(PasswordPolicy.valid("12345678"));
        assertFalse(PasswordPolicy.valid(null));
        assertTrue(PasswordPolicy.valid("A1" + "x".repeat(70)));
        assertFalse(PasswordPolicy.valid("A1" + "x".repeat(71)));
        assertFalse(PasswordPolicy.valid("A1" + "\u00e9".repeat(36)));
        assertTrue(PasswordPolicy.validAdministrator("Administrador2026!"));
        assertFalse(PasswordPolicy.validAdministrator("Prueba12"));
    }

    @Test void paginationCannotOverflowIntegerRange() {
        assertEquals(0, PageWindow.offset(-1, 20));
        assertEquals(40, PageWindow.offset(2, 20));
        assertEquals(107374182350L, PageWindow.offset(Integer.MAX_VALUE, 50));
        assertThrows(IllegalArgumentException.class, () -> PageWindow.offset(1, 0));
    }

    @Test void storagePathsMustBeGeneratedObjectKeys() {
        String key = "12/550e8400-e29b-41d4-a716-446655440000.png";
        assertEquals(key, ObjectKey.create(12, "550e8400-e29b-41d4-a716-446655440000"));
        for (String invalid : List.of("../secret", "/etc/passwd", "https://example.com/a", "1/a.png", key + "?q=1")) {
            assertThrows(IllegalArgumentException.class, () -> ObjectKey.require(invalid));
        }
    }

    @Test void eventRecipientsAreAnImmutableSnapshot() {
        var recipients = new ArrayList<>(List.of(1L, 2L));
        var event = DomainEvent.of("CREATED", null, 1L, "Title", "Detail", recipients);
        recipients.clear();
        assertEquals(List.of(1L, 2L), event.recipients());
        assertThrows(UnsupportedOperationException.class, () -> event.recipients().add(3L));
    }

    @Test void limiterExpiresAndNeverExceedsCapacity() {
        var clock = new MutableClock();
        var limiter = new FixedWindowRateLimiter(clock, Duration.ofMinutes(1), 2);
        assertTrue(limiter.allow("a", 2)); assertTrue(limiter.allow("a", 2));
        assertFalse(limiter.allow("a", 2)); assertTrue(limiter.allow("b", 2));
        assertFalse(limiter.allow("c", 2)); assertEquals(2, limiter.size());
        clock.instant = clock.instant.plusSeconds(60);
        assertTrue(limiter.allow("c", 2)); assertTrue(limiter.size() <= 2);
    }

    @Test void limiterSerializesConcurrentRequests() throws Exception {
        var limiter = new FixedWindowRateLimiter(Clock.systemUTC(), Duration.ofMinutes(1), 10);
        try (var pool = Executors.newFixedThreadPool(8)) {
            var calls = new ArrayList<java.util.concurrent.Callable<Boolean>>();
            for (int i = 0; i < 100; i++) calls.add(() -> limiter.allow("same", 7));
            long permitted = 0;
            for (var result : pool.invokeAll(calls)) if (result.get()) permitted++;
            assertEquals(7, permitted);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-01-01T00:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
