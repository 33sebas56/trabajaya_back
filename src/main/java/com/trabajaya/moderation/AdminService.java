package com.trabajaya.moderation;

import com.trabajaya.moderation.AdminRequests.*;
import com.trabajaya.shared.*;
import com.trabajaya.jobs.JobRepository;
import com.trabajaya.events.DomainEvent;
import static com.trabajaya.shared.Db.*;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import java.util.*;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminService {
    private final Db db;
    private final JobRepository jobs;
    private final ApplicationEventPublisher events;

    public AdminService(Db db, JobRepository jobs, ApplicationEventPublisher events) {
        this.db = db;
        this.jobs = jobs;
        this.events = events;
    }

    public Object summary() {
        return Map.of("users", db.count("SELECT COUNT(*) FROM users", Map.of()), "jobs", db.count("SELECT COUNT(*) FROM jobs", Map.of()), "completed", db.count("SELECT COUNT(*) FROM jobs WHERE status='COMPLETADO'", Map.of()), "reports", db.count("SELECT COUNT(*) FROM reports WHERE status<>'RESUELTO'", Map.of()));
    }

    public Object users(int page) {
        return db.list("SELECT id,name,email,role,city,active,created_at FROM users ORDER BY id DESC LIMIT 100 OFFSET :offset", p("offset", PageWindow.offset(page, 100)));
    }

    @Transactional
    public Object active(long id, Principal p, Active r) {
        if (id == p.id()) throw ApiException.bad("No puedes suspender tu propia cuenta.");
        var u = db.require("SELECT * FROM users WHERE id=:id FOR UPDATE", p("id", id));
        if (text(u, "role").equals("ADMIN")) throw ApiException.bad("Los administradores se gestionan fuera de este panel.");
        db.update("UPDATE users SET active=:active WHERE id=:id", p("active", r.active(), "id", id));
        if (!r.active()) db.update("DELETE FROM auth_sessions WHERE user_id=:id", p("id", id));
        events.publishEvent(DomainEvent.of("USER_MODERATED", p.id(), null, "Estado de cuenta actualizado", r.reason(), List.of(id)));
        return Map.of("message", "Cuenta actualizada.");
    }

    public Object jobs(int page) {
        return db.list("SELECT j.id,j.title,j.status,j.hidden,j.current_price,j.city,u.name AS client_name FROM jobs j JOIN users u ON u.id=j.client_id ORDER BY j.id DESC LIMIT 100 OFFSET :offset", p("offset", PageWindow.offset(page, 100)));
    }

    @Transactional
    public Object hide(long id, Principal p, Active r) {
        var j = jobs.lock(id);
        var people = jobs.related(id);
        db.update("UPDATE jobs SET hidden=TRUE,auto_increase=FALSE WHERE id=:id", p("id", id));
        jobs.event(id, null, p.id(), "MODERADO", r.reason());
        events.publishEvent(DomainEvent.of("JOB_MODERATED", p.id(), id, "Trabajo suspendido por moderacion", r.reason(), people));
        return Map.of("message", "Publicacion suspendida. Los participantes conservan el historial y pueden reportar el caso.");
    }

    public Object reports() {
        return db.list("SELECT r.*,u.name AS author_name,j.title AS job_title FROM reports r JOIN users u ON u.id=r.author_id JOIN jobs j ON j.id=r.job_id ORDER BY r.id DESC LIMIT 100");
    }

    @Transactional
    public Object resolve(long id, Principal p, Resolve r) {
        var report = db.require("SELECT * FROM reports WHERE id=:id FOR UPDATE", p("id", id));
        db.update("UPDATE reports SET status=:status,resolution=:resolution,resolved_by=:admin,updated_at=CURRENT_TIMESTAMP WHERE id=:id", p("id", id, "status", r.status(), "resolution", r.resolution().trim(), "admin", p.id()));
        events.publishEvent(DomainEvent.of("REPORT_UPDATED", p.id(), id(report, "job_id"), "Tu reporte #" + id + " fue actualizado", "Estado: " + r.status(), List.of(id(report, "author_id"))));
        return Map.of("message", "Caso actualizado.");
    }

    public Object categories() {
        return db.list("SELECT * FROM categories ORDER BY name");
    }

    @Transactional
    public Object category(Principal p, Category r) {
        long id = db.insert("INSERT INTO categories(name,active) VALUES(:name,:active)", p("name", r.name().trim(), "active", r.active()));
        audit(p, "Categoria creada: " + r.name());
        return Map.of("id", id);
    }

    @Transactional
    public Object updateCategory(long id, Principal p, Category r) {
        if (db.update("UPDATE categories SET name=:name,active=:active WHERE id=:id", p("name", r.name().trim(), "active", r.active(), "id", id)) == 0) throw ApiException.missing();
        audit(p, "Categoria actualizada: " + r.name());
        return Map.of("message", "Categoria actualizada.");
    }

    private void audit(Principal p, String detail) {
        events.publishEvent(DomainEvent.of("CATEGORY_UPDATED", p.id(), null, "Catalogo actualizado", detail, List.of()));
    }

    public Object audit(int page) {
        return db.list("SELECT e.*,u.name AS actor_name FROM domain_events e LEFT JOIN users u ON u.id=e.actor_id ORDER BY e.created_at DESC LIMIT 100 OFFSET :offset", p("offset", PageWindow.offset(page, 100)));
    }
}
