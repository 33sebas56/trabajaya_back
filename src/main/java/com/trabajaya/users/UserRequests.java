package com.trabajaya.users;

import jakarta.validation.constraints.*;
import java.util.List;

/** Request contracts shared with the existing frontend. */
public final class UserRequests {
    private UserRequests() { }

    public record Register(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 180) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotNull @Pattern(regexp = "CLIENTE|TRABAJADOR") String role,
            @NotBlank @Size(max = 30) String phone,
            @NotBlank @Size(max = 80) String city,
            @NotNull @Size(max = 20) List<@NotNull @Positive Long> skills,
            @Min(0) @Max(60) int experience,
            @Min(1) @Max(100) int radiusKm,
            @AssertTrue(message = "Debes aceptar los terminos de uso.") boolean acceptedTerms) { }

    public record Login(
            @NotBlank @Email @Size(max = 180) String email,
            @NotBlank @Size(max = 72) String password) { }

    public record Forgot(@NotBlank @Email @Size(max = 180) String email) { }

    public record Reset(
            @NotBlank @Size(max = 100) String token,
            @NotBlank @Size(min = 8, max = 72) String password) { }

    public record Profile(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 30) String phone,
            @NotBlank @Size(max = 80) String city,
            @NotNull @Size(max = 1200) String bio,
            @Min(0) @Max(60) int experience,
            @NotNull @Size(max = 20) List<@NotNull @Positive Long> skills,
            @Min(1) @Max(100) int radiusKm,
            boolean available,
            Double latitude,
            Double longitude,
            @Size(max = 40) String avatarId) { }
}
