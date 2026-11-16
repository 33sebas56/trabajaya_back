package com.trabajaya.messaging;

import com.trabajaya.shared.Principal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** HTTP adapter. Business rules and transactions live in the service. */
@RestController
@RequestMapping("/api/assignments/{assignment}/messages")
public class ChatController {

    private final ChatService service;

    public ChatController(ChatService service) { this.service = service; }

    @GetMapping
    public Object list(@PathVariable long assignment, @AuthenticationPrincipal Principal user,
                       @RequestParam(defaultValue = "0") long after) {
        return service.list(assignment, user, after);
    }

    @PostMapping
    public Object send(@PathVariable long assignment, @AuthenticationPrincipal Principal user,
                       @Valid @RequestBody ChatRequests.Send request) {
        return service.send(assignment, user, request);
    }

    @GetMapping("/read-status")
    public Object readStatus(@PathVariable long assignment, @AuthenticationPrincipal Principal user) {
        return service.readStatus(assignment, user);
    }

    @PostMapping("/read")
    public Object read(@PathVariable long assignment, @AuthenticationPrincipal Principal user) {
        return service.read(assignment, user);
    }
}
