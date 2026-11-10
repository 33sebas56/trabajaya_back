package com.trabajaya.notifications;
import com.trabajaya.shared.*;
import static com.trabajaya.shared.Db.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/notifications")
public class NotificationController {
    private final Db db;
    public NotificationController(Db db){
        this.db=db;
    }
    @GetMapping public Object list(@AuthenticationPrincipal Principal p){
        return Map.of("items",db.list("SELECT * FROM notifications WHERE user_id=:id ORDER BY id DESC LIMIT 100",p("id",p.id())),"unread",db.count("SELECT COUNT(*) FROM notifications WHERE user_id=:id AND is_read=FALSE",p("id",p.id())));
    }
    @PostMapping("/{id}/read") public Object read(@PathVariable long id,@AuthenticationPrincipal Principal p){
        if(db.update("UPDATE notifications SET is_read=TRUE WHERE id=:id AND user_id=:user",p("id",id,"user",p.id()))==0)throw ApiException.missing();
        return Map.of("message","Notificacion leida.");
    }
    @PostMapping("/read-all") public Object all(@AuthenticationPrincipal Principal p){
        db.update("UPDATE notifications SET is_read=TRUE WHERE user_id=:id",p("id",p.id()));
        return Map.of("message","Notificaciones leidas.");
    }
}
