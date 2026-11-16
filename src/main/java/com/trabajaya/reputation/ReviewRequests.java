package com.trabajaya.reputation;

import jakarta.validation.constraints.*;

/** Validated HTTP request contracts. */
public final class ReviewRequests {
    private ReviewRequests() { }

    public record Rate(@Min(1) @Max(5) int score,
            @NotNull @Size(max=1500) String comment){
    }
}
