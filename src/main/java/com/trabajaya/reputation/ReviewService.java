package com.trabajaya.reputation;

import com.trabajaya.reputation.ReviewRequests.*;
import com.trabajaya.shared.*;
import com.trabajaya.jobs.JobRepository;
import com.trabajaya.events.DomainEvent;
import static com.trabajaya.shared.Db.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import java.util.*;

@Service
public class ReviewService {
    private final Db db;
    private final JobRepository jobs;
    private final ApplicationEventPublisher events;

    public ReviewService(Db db, JobRepository jobs, ApplicationEventPublisher events) {
        this.db = db;
        this.jobs = jobs;
        this.events = events;
    }

    @Transactional
    public Object rate(long assignment, Principal p, Rate r) {
        var a = jobs.assignment(assignment);
        if (!jobs.participant(a, p)) throw ApiException.forbidden();
        if (!text(a, "status").equals("CONFIRMADO")) throw ApiException.conflict("La participacion debe estar confirmada para calificar.");
        long target = p.id() == id(a, "client_id") ? id(a, "worker_id") : id(a, "client_id");
        long id = db.insert("INSERT INTO reviews(assignment_id,author_id,target_id,score,comment) VALUES(:assignment,:author,:target,:score,:comment)", p("assignment", assignment, "author", p.id(), "target", target, "score", r.score(), "comment", r.comment().trim()));
        events.publishEvent(DomainEvent.of("REVIEW_CREATED", p.id(), id(a, "job_id"), "Recibiste una calificacion", "Tu perfil tiene una nueva opinion.", List.of(target)));
        return Map.of("id", id, "message", "Calificacion guardada.");
    }
}
