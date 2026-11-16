package com.trabajaya.users;

import com.trabajaya.shared.ApiException;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Bounded in-memory protection, not a distributed anti-abuse service. */
@Component
public class AuthRateLimiter {
    private final FixedWindowRateLimiter buckets;
    private final boolean enabled;
    private final int sourceLimit;
    private final int identityLimit;

    public AuthRateLimiter(
            @Value("${app.auth.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.auth.rate-limit.source-limit:100}") int sourceLimit,
            @Value("${app.auth.rate-limit.identity-limit:20}") int identityLimit) {
        this.enabled = enabled;
        this.sourceLimit = sourceLimit;
        this.identityLimit = identityLimit;
        this.buckets = new FixedWindowRateLimiter(Clock.systemUTC(), Duration.ofMinutes(1), 10000);
        if (sourceLimit < 1 || identityLimit < 1) throw new IllegalArgumentException("Invalid rate limits");
    }

    public void check(String source, String action, String identity) {
        if (!enabled) return;
        // Do not trust a caller-supplied X-Forwarded-For header here.
        String origin = action + ":ip:" + AuthService.hash(source);
        String account = action + ":identity:" + AuthService.hash(identity.trim().toLowerCase(java.util.Locale.ROOT));
        if (!buckets.allow(origin, sourceLimit) || !buckets.allow(account, identityLimit)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos. Espera un minuto antes de continuar.");
        }
    }
}
