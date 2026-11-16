package com.trabajaya.notifications;

import com.trabajaya.shared.Principal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** HTTP adapter. Business rules and transactions live in the service. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) { this.service = service; }

    @GetMapping
    public Object list(@AuthenticationPrincipal Principal user) { return service.list(user); }

    @PostMapping("/{id}/read")
    public Object read(@PathVariable long id, @AuthenticationPrincipal Principal user) {
        return service.read(id, user);
    }

    @PostMapping("/read-all")
    public Object all(@AuthenticationPrincipal Principal user) { return service.all(user); }
}
