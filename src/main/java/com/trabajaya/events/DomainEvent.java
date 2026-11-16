package com.trabajaya.events;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Immutable event, handled synchronously inside the business transaction. */
public record DomainEvent(
        String id, String kind, Long actorId, Long jobId,
        String title, String detail, List<Long> recipients) {

    public DomainEvent {
        Objects.requireNonNull(id);
        Objects.requireNonNull(kind);
        recipients = List.copyOf(recipients);
    }

    public static DomainEvent of(String kind, Long actor, Long job, String title,
                                 String detail, List<Long> recipients) {
        return new DomainEvent(UUID.randomUUID().toString(), kind, actor, job,
                title, detail, recipients);
    }
}
