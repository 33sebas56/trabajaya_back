package com.trabajaya.storage;

import com.trabajaya.shared.*;
import com.trabajaya.jobs.*;
import static com.trabajaya.shared.Db.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@Service
public class MediaService {
    private final ObjectStorage storage;
    private final ImageSanitizer sanitizer;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MediaService.class);
    private final Db db;
    private final JobService jobs;
    private final JobRepository repo;

    public MediaService(ObjectStorage storage, Db db, JobService jobs, JobRepository repo, ImageSanitizer sanitizer) {
        this.storage = storage;
        this.sanitizer = sanitizer;
        this.db = db;
        this.jobs = jobs;
        this.repo = repo;
    }

    @Transactional
    public Object upload(Principal p, MultipartFile file) {
        byte[] sanitized = sanitizer.sanitize(file);
        String id = UUID.randomUUID().toString();
        String path = ObjectKey.create(p.id(), id);
        storage.write(path, sanitized, "image/png");
        boolean synchronizedTransaction = org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive();
        if (synchronizedTransaction) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){

                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) compensate(path);
                }
            });
        }
        try {
            db.update("INSERT INTO media(id,owner_id,object_path,provider,content_type,size_bytes) VALUES(:id,:owner,:path,:provider,'image/png',:size)", p("id", id, "owner", p.id(), "path", path, "provider", storage.provider(), "size", sanitized.length));
        } catch (RuntimeException e) {
            if (!synchronizedTransaction) compensate(path);
            throw e;
        }
        return Map.of("id", id, "url", "/api/media/" + id, "contentType", "image/png", "size", sanitized.length);
    }

    private void compensate(String path) {
        try {
            storage.delete(path);
        } catch (RuntimeException error) {
            log.warn("Could not clean up rolled-back object {}; reconcile storage later", path);
        }
    }

    public byte[] read(String id, Principal p) {
        var m = db.require("SELECT * FROM media WHERE id=:id", p("id", id));
        boolean allowed = id(m, "owner_id") == p.id() || p.is("ADMIN");
        if (!allowed && m.get("job_id") != null) {
            jobs.visible(repo.get(id(m, "job_id")), p);
            allowed = true;
        }
        if (!allowed && m.get("assignment_id") != null) allowed = repo.participant(repo.assignment(id(m, "assignment_id")), p);
        if (!allowed) allowed = db.count("SELECT COUNT(*) FROM users WHERE avatar_id=:id AND active=TRUE", p("id", id)) > 0;
        if (!allowed) throw ApiException.forbidden();
        if (!text(m, "provider").equals(storage.provider())) throw ApiException.conflict("Esta imagen pertenece a otro proveedor. Conserva el proveedor anterior o migra sus archivos antes de cambiarlo.");
        return storage.read(text(m, "object_path"));
    }

    @Transactional
    public void attachChat(String id, long assignment, Principal p) {
        var m = db.require("SELECT * FROM media WHERE id=:id FOR UPDATE", p("id", id));
        if (id(m, "owner_id") != p.id() || m.get("job_id") != null || m.get("assignment_id") != null || db.count("SELECT COUNT(*) FROM users WHERE avatar_id=:id", p("id", id)) > 0) throw ApiException.forbidden();
        db.update("UPDATE media SET assignment_id=:assignment WHERE id=:id", p("assignment", assignment, "id", id));
    }
}
