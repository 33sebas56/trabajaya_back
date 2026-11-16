package com.trabajaya.messaging;

import jakarta.validation.constraints.*;

/** Validated HTTP request contracts. */
public final class ChatRequests {
    private ChatRequests() { }

    public record Send(@NotNull @Size(max=2000) String body,
            @Size(max=40) String mediaId){
    }
}
