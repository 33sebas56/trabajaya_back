package com.trabajaya.reputation;

import com.trabajaya.shared.Principal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** HTTP adapter. Business rules and transactions live in the service. */
@RestController
@RequestMapping("/api/assignments/{assignment}/review")
public class ReviewController {

    private final ReviewService service;

    public ReviewController(ReviewService service) { this.service = service; }

    @PostMapping
    public Object rate(@PathVariable long assignment, @AuthenticationPrincipal Principal user,
                       @Valid @RequestBody ReviewRequests.Rate request) {
        return service.rate(assignment, user, request);
    }
}
