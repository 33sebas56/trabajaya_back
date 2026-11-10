package com.trabajaya.jobs;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.List;
public final class JobRequests {
    public record Publish(@NotBlank @Size(max=150) String title,@NotBlank @Size(min=15,max=4000) String description,
    @NotNull Long categoryId,@NotBlank @Size(max=80) String city,@NotBlank @Size(max=120) String zone,
    @NotBlank @Size(max=240) String address,Double latitude,Double longitude,
    @NotNull @Future OffsetDateTime scheduledAt,@Min(1) @Max(24) int durationHours,@Min(1) @Max(10) int workersNeeded,
    @Min(1000) @Max(100000000) long initialPrice,@Min(1000) @Max(100000000) long maxPrice,
    @Min(1) @Max(100000000) long incrementAmount,@Min(1) @Max(1440) int intervalMinutes,
    boolean autoIncrease,@NotNull @Size(max=5) List<String> mediaIds){
    }
    public record Edit(@NotBlank @Size(max=150) String title,@NotBlank @Size(min=15,max=4000) String description,
    @NotNull Long categoryId,@NotBlank @Size(max=80) String city,@NotBlank @Size(max=120) String zone,
    @NotBlank @Size(max=240) String address,Double latitude,Double longitude,
    @NotNull @Future OffsetDateTime scheduledAt,@Min(1) @Max(24) int durationHours,@Min(1) @Max(10) int workersNeeded,
    @NotNull @Size(max=5) List<String> mediaIds){
    }
    public record Increase(@Min(1) @Max(100000000) long amount){
    }
    public record Cancel(@NotBlank @Size(min=5,max=1000) String reason){
    }
    public record Accept(@Min(1) long expectedPrice){
    }
    public record State(@NotBlank String status){
    }
}
