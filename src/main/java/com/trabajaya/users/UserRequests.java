package com.trabajaya.users;
import jakarta.validation.constraints.*;
import java.util.List;
public final class UserRequests {
    public record Register(@NotBlank @Size(max=100) String name,@NotBlank @Email @Size(max=180) String email,
    @NotBlank @Size(min=8,max=72) String password,@Pattern(regexp="CLIENTE|TRABAJADOR") @NotNull String role,
    @NotBlank @Size(max=30) String phone,@NotBlank @Size(max=80) String city,
    @NotNull List<Long> skills,@Min(0) @Max(60) int experience,@Min(1) @Max(100) int radiusKm,
    @AssertTrue(message="Debes aceptar los terminos de uso del prototipo.") boolean acceptedTerms){
    }
    public record Login(@NotBlank @Email String email,@NotBlank String password){
    }
    public record Forgot(@NotBlank @Email @Size(max=180) String email){
    }
    public record Reset(@NotBlank String token,@NotBlank @Size(min=8,max=72) String password){
    }
    public record Profile(@NotBlank @Size(max=100) String name,@NotBlank @Size(max=30) String phone,@NotBlank @Size(max=80) String city,
    @NotNull @Size(max=1200) String bio,@Min(0) @Max(60) int experience,@NotNull List<Long> skills,
    @Min(1) @Max(100) int radiusKm,boolean available,Double latitude,Double longitude,String avatarId){
    }
}
