package com.trabajaya.moderation;

import jakarta.validation.constraints.*;

/** Validated HTTP request contracts. */
public final class AdminRequests {
    private AdminRequests() { }

    public record Active(boolean active,
            @NotBlank @Size(min=5,max=1000)String reason){
    }

    public record Category(@NotBlank @Size(max=80)String name,boolean active){
    }

    public record Resolve(@NotNull @Pattern(regexp="EN_REVISION|RESUELTO")String status,
            @NotBlank @Size(min=5,max=2500)String resolution){
    }
}
