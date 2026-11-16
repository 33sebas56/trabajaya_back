package com.trabajaya.moderation;

import com.trabajaya.moderation.ReportRequests.*;
import com.trabajaya.shared.*;
import com.trabajaya.jobs.*;
import com.trabajaya.events.DomainEvent;
import static com.trabajaya.shared.Db.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import java.util.*;

@Service
public class ReportService {
    private final Db db;
    private final JobService jobs;
    private final JobRepository repo;
    private final ApplicationEventPublisher events;

    public ReportService(Db db, JobService jobs, JobRepository repo, ApplicationEventPublisher events) {
        this.db = db;
        this.jobs = jobs;
        this.repo = repo;
        this.events = events;
    }

    public Object mine(Principal p) {
        return db.list("SELECT r.*,j.title AS job_title FROM reports r JOIN jobs j ON j.id=r.job_id WHERE r.author_id=:id ORDER BY r.id DESC LIMIT 100", p("id", p.id()));
    }

    @Transactional
    public Object report(Principal p, Report r) {
        jobs.visible(repo.get(r.jobId()), p);
        long id = db.insert("INSERT INTO reports(author_id,job_id,reason,detail) VALUES(:author,:job,:reason,:detail)", p("author", p.id(), "job", r.jobId(), "reason", r.reason(), "detail", r.detail().trim()));
        var admins = db.list("SELECT id FROM users WHERE role='ADMIN' AND active=TRUE").stream().map((a)->id(a, "id")).toList();
        events.publishEvent(DomainEvent.of("REPORT_CREATED", p.id(), r.jobId(), "Nuevo reporte #" + id, "Revisa el caso desde administracion.", admins));
        return Map.of("id", id, "message", "Reporte registrado. No es un canal de atencion de emergencias.");
    }
}
