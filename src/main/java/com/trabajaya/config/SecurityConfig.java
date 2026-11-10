package com.trabajaya.config;
import com.trabajaya.users.AuthFilter;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.*;
@Configuration @EnableWebSecurity
public class SecurityConfig {
    @Bean org.springframework.boot.web.servlet.FilterRegistrationBean<AuthFilter> disableContainerFilter(AuthFilter filter){
        var registration=new org.springframework.boot.web.servlet.FilterRegistrationBean<AuthFilter>(filter);
        registration.setEnabled(false);
        return registration;
    }
    @Bean PasswordEncoder encoder(){
        return new BCryptPasswordEncoder(12);
    }
    @Bean SecurityFilterChain chain(HttpSecurity http,AuthFilter filter)throws Exception{
        return http.csrf(c->c.disable()) // Only explicit Bearer headers; no cookie authentication.
        .cors(c->{
        }
        ).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a->a.requestMatchers("/error","/actuator/health","/api/health","/api/categories","/api/auth/register","/api/auth/login","/api/auth/forgot-password","/api/auth/reset-password").permitAll().requestMatchers("/api/admin/**").hasRole("ADMIN").anyRequest().authenticated())
        .exceptionHandling(e->e.authenticationEntryPoint((req,res,x)->{
            res.setStatus(401);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\"Inicia sesion para continuar.\"}");
        }
        ).accessDeniedHandler((req,res,x)->{
            res.setStatus(403);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\"No tienes permiso para esta accion.\"}");
        }
        ))
        .addFilterBefore(filter,UsernamePasswordAuthenticationFilter.class).build();
    }
    @Bean CorsConfigurationSource corsConfigurationSource(@Value("${app.cors-origins}")String origins){
        var c=new CorsConfiguration();
        c.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());
        c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization","Content-Type"));
        c.setAllowCredentials(false);
        var s=new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**",c);
        return s;
    }
}
