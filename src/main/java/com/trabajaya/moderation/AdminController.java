package com.trabajaya.moderation;

import com.trabajaya.shared.Principal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** HTTP adapter. Business rules and transactions live in the service. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService service;

    public AdminController(AdminService service) { this.service = service; }

    @GetMapping("/summary")
    public Object summary() { return service.summary(); }

    @GetMapping("/users")
    public Object users(@RequestParam(defaultValue = "0") int page) { return service.users(page); }

    @PatchMapping("/users/{id}")
    public Object active(@PathVariable long id, @AuthenticationPrincipal Principal user,
                         @Valid @RequestBody AdminRequests.Active request) {
        return service.active(id, user, request);
    }

    @GetMapping("/jobs")
    public Object jobs(@RequestParam(defaultValue = "0") int page) { return service.jobs(page); }

    @PostMapping("/jobs/{id}/hide")
    public Object hide(@PathVariable long id, @AuthenticationPrincipal Principal user,
                       @Valid @RequestBody AdminRequests.Active request) {
        return service.hide(id, user, request);
    }

    @GetMapping("/reports")
    public Object reports() { return service.reports(); }

    @PatchMapping("/reports/{id}")
    public Object resolve(@PathVariable long id, @AuthenticationPrincipal Principal user,
                          @Valid @RequestBody AdminRequests.Resolve request) {
        return service.resolve(id, user, request);
    }

    @GetMapping("/categories")
    public Object categories() { return service.categories(); }

    @PostMapping("/categories")
    public Object category(@AuthenticationPrincipal Principal user,
                           @Valid @RequestBody AdminRequests.Category request) {
        return service.category(user, request);
    }

    @PutMapping("/categories/{id}")
    public Object updateCategory(@PathVariable long id, @AuthenticationPrincipal Principal user,
                                 @Valid @RequestBody AdminRequests.Category request) {
        return service.updateCategory(id, user, request);
    }

    @GetMapping("/audit")
    public Object audit(@RequestParam(defaultValue = "0") int page) { return service.audit(page); }
}
