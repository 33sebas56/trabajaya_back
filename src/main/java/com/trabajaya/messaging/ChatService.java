package com.trabajaya.messaging;

import com.trabajaya.messaging.ChatRequests.*;
import com.trabajaya.shared.*;
import com.trabajaya.jobs.JobRepository;
import com.trabajaya.storage.MediaService;
import com.trabajaya.events.DomainEvent;
import static com.trabajaya.shared.Db.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import java.util.*;

@Service
public class ChatService {
    private final Db db;
    private final JobRepository jobs;
    private final MediaService media;
    private final ApplicationEventPublisher events;

    public ChatService(Db db, JobRepository jobs, MediaService media, ApplicationEventPublisher events) {
        this.db = db;
        this.jobs = jobs;
        this.media = media;
        this.events = events;
    }

    private Map<String, Object> access(long id, Principal p) {
        var a = jobs.assignment(id);
        if (!jobs.participant(a, p)) throw ApiException.forbidden();
        return a;
    }

    public Object list(long assignment, Principal p, long after) {
        access(assignment, p);
        return db.list("SELECT m.*,u.name AS sender_name FROM messages m JOIN users u ON u.id=m.sender_id WHERE m.assignment_id=:assignment AND m.id>:after ORDER BY m.id LIMIT 100", p("assignment", assignment, "after", Math.max(0, after)));
    }

    @Transactional
    public Object send(long assignment, Principal p, Send r) {
        var a = access(assignment, p);
        jobs.lock(id(a, "job_id"));
        a = access(assignment, p);
        if (Set.of("CANCELADO", "CONFIRMADO").contains(text(a, "status")) || bool(a, "hidden")) throw ApiException.conflict("El chat de este trabajo esta cerrado. Puedes consultar su historial.");
        if (r.body().isBlank() && (r.mediaId() == null || r.mediaId().isBlank())) throw ApiException.bad("Escribe un mensaje o adjunta una imagen.");
        if (r.mediaId() != null && !r.mediaId().isBlank()) media.attachChat(r.mediaId(), assignment, p);
        long id = db.insert("INSERT INTO messages(assignment_id,sender_id,body,media_id) VALUES(:assignment,:sender,:body,:media)", p("assignment", assignment, "sender", p.id(), "body", r.body().trim(), "media", r.mediaId() == null || r.mediaId().isBlank() ? null : r.mediaId()));
        long other = id(a, "client_id") == p.id() ? id(a, "worker_id") : id(a, "client_id");
        events.publishEvent(DomainEvent.of("MESSAGE_SENT", p.id(), id(a, "job_id"), "Nuevo mensaje", p.name() + " envio un mensaje en " + text(a, "title"), List.of(other)));
        return db.require("SELECT m.*,u.name AS sender_name FROM messages m JOIN users u ON u.id=m.sender_id WHERE m.id=:id", p("id", id));
    }

    public Object readStatus(long assignment, Principal p) {
        access(assignment, p);
        return db.require("SELECT COALESCE(MAX(id),0) AS last_read_id FROM messages WHERE assignment_id=:assignment AND sender_id=:user AND read_at IS NOT NULL", p("assignment", assignment, "user", p.id()));
    }

    public Object read(long assignment, Principal p) {
        access(assignment, p);
        db.update("UPDATE messages SET read_at=CURRENT_TIMESTAMP WHERE assignment_id=:assignment AND sender_id<>:user AND read_at IS NULL", p("assignment", assignment, "user", p.id()));
        return Map.of("message", "Mensajes leidos.");
    }
}
