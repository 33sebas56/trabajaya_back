package com.trabajaya.notifications;

import com.trabajaya.shared.*;
import static com.trabajaya.shared.Db.*;
import org.springframework.stereotype.Service;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final Db db;

    public NotificationService(Db db) {
        this.db = db;
    }

    public Object list(Principal p) {
        return Map.of("items", db.list("SELECT * FROM notifications WHERE user_id=:id ORDER BY id DESC LIMIT 100", p("id", p.id())), "unread", db.count("SELECT COUNT(*) FROM notifications WHERE user_id=:id AND is_read=FALSE", p("id", p.id())));
    }

    @Transactional
    public Object read(long id, Principal p) {
        if (db.update("UPDATE notifications SET is_read=TRUE WHERE id=:id AND user_id=:user", p("id", id, "user", p.id())) == 0) throw ApiException.missing();
        return Map.of("message", "Notificacion leida.");
    }

    @Transactional
    public Object all(Principal p) {
        db.update("UPDATE notifications SET is_read=TRUE WHERE user_id=:id", p("id", p.id()));
        return Map.of("message", "Notificaciones leidas.");
    }
}
