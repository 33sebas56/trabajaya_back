package com.trabajaya.moderation;

import jakarta.validation.constraints.*;

/** Validated HTTP request contracts. */
public final class ReportRequests {
    private ReportRequests() { }

    public record Report(@Min(1) long jobId,
            @NotNull @Pattern(regexp="INCUMPLIMIENTO|SEGURIDAD|FRAUDE|OTRO")String reason,
            @NotBlank @Size(min=10,max=2500)String detail){
    }
}
