package com.trabajaya.users;

import com.trabajaya.shared.Principal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** HTTP adapter. Business rules and transactions live in the service. */
@RestController
@RequestMapping("/api")
public class ProfileController {

    private final ProfileService service;

    public ProfileController(ProfileService service) { this.service = service; }

    @GetMapping("/dashboard")
    public Object dashboard(@AuthenticationPrincipal Principal user) { return service.dashboard(user); }

    @GetMapping("/me")
    public Object me(@AuthenticationPrincipal Principal user) { return service.me(user); }

    @GetMapping("/users/{id}")
    public Object publicProfile(@PathVariable long id) { return service.publicProfile(id); }

    @PutMapping("/me")
    public Object update(@AuthenticationPrincipal Principal user,
                         @Valid @RequestBody UserRequests.Profile request) {
        return service.update(user, request);
    }
}
