package com.trabajaya.users;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final AuthRateLimiter limiter;

    public AuthController(AuthService auth, AuthRateLimiter limiter) {
        this.auth = auth;
        this.limiter = limiter;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Object register(@Valid @RequestBody UserRequests.Register body, HttpServletRequest request) {
        limiter.check(request.getRemoteAddr(), "register", body.email());
        return auth.register(body);
    }

    @PostMapping("/login")
    public Object login(@Valid @RequestBody UserRequests.Login body, HttpServletRequest request) {
        limiter.check(request.getRemoteAddr(), "login", body.email());
        return auth.login(body);
    }

    @PostMapping("/logout")
    public Object logout(@RequestHeader(value = "Authorization", required = false) String header) {
        auth.logout(header);
        return Map.of("message", "Sesion cerrada.");
    }

    @PostMapping("/forgot-password")
    public Object forgot(@Valid @RequestBody UserRequests.Forgot body, HttpServletRequest request) {
        limiter.check(request.getRemoteAddr(), "forgot", body.email());
        auth.forgot(body.email());
        return Map.of("message", "Si la cuenta existe y la recuperacion esta disponible, recibiras un enlace.");
    }

    @PostMapping("/reset-password")
    public Object reset(@Valid @RequestBody UserRequests.Reset body, HttpServletRequest request) {
        limiter.check(request.getRemoteAddr(), "reset", body.token());
        auth.reset(body);
        return Map.of("message", "Clave actualizada. Inicia sesion nuevamente.");
    }
}
