package com.trabajaya.moderation;

import com.trabajaya.shared.Principal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** HTTP adapter. Business rules and transactions live in the service. */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) { this.service = service; }

    @GetMapping
    public Object mine(@AuthenticationPrincipal Principal user) { return service.mine(user); }

    @PostMapping
    public Object report(@AuthenticationPrincipal Principal user,
                         @Valid @RequestBody ReportRequests.Report request) {
        return service.report(user, request);
    }
}
