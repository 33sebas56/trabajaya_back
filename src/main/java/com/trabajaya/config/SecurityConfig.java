package com.trabajaya.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trabajaya.shared.RequestIdFilter;
import com.trabajaya.users.AuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    FilterRegistrationBean<AuthFilter> disableContainerFilter(AuthFilter filter) {
        var registration = new FilterRegistrationBean<AuthFilter>(filter);
        registration.setEnabled(false); // Register once, inside the security chain.
        return registration;
    }

    @Bean
    PasswordEncoder encoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    SecurityFilterChain chain(HttpSecurity http, AuthFilter filter, ObjectMapper json) throws Exception {
        return http
                // The API uses explicit Bearer headers, not cookie authentication.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> { })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error", "/actuator/health", "/api/health", "/api/categories",
                                "/api/auth/register", "/api/auth/login", "/api/auth/forgot-password",
                                "/api/auth/reset-password").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((req, res, error) ->
                                securityError(json, req, res, 401, "Inicia sesion para continuar."))
                        .accessDeniedHandler((req, res, error) ->
                                securityError(json, req, res, 403, "No tienes permiso para esta accion.")))
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private void securityError(ObjectMapper json, HttpServletRequest request, HttpServletResponse response,
                               int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        var body = new LinkedHashMap<String, Object>();
        body.put("message", message);
        body.put("status", status);
        body.put("requestId", request.getAttribute(RequestIdFilter.ATTRIBUTE));
        json.writeValue(response.getOutputStream(), body);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors-origins}") String origins) {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim)
                .filter(value -> !value.isEmpty()).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setExposedHeaders(List.of("X-Request-ID", "Retry-After"));
        cors.setAllowCredentials(false);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
