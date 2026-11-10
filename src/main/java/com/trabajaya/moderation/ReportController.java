package com.trabajaya.moderation;
import com.trabajaya.shared.*;
import com.trabajaya.jobs.*;
import com.trabajaya.events.DomainEvent;
import static com.trabajaya.shared.Db.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import java.util.*;
@RestController @RequestMapping("/api/reports")
public class ReportController {
    private final Db db;
    private final JobService jobs;
    private final JobRepository repo;
    private final ApplicationEventPublisher events;
    public record Report(@Min(1) long jobId,@NotNull @Pattern(regexp="INCUMPLIMIENTO|SEGURIDAD|FRAUDE|OTRO")String reason,@NotBlank @Size(min=10,max=2500)String detail){
    }
    public ReportController(Db db,JobService jobs,JobRepository repo,ApplicationEventPublisher events){
        this.db=db;
        this.jobs=jobs;
        this.repo=repo;
        this.events=events;
    }
    @GetMapping public Object mine(@AuthenticationPrincipal Principal p){
        return db.list("SELECT r.*,j.title AS job_title FROM reports r JOIN jobs j ON j.id=r.job_id WHERE r.author_id=:id ORDER BY r.id DESC LIMIT 100",p("id",p.id()));
    }
    @PostMapping @Transactional public Object report(@AuthenticationPrincipal Principal p,@Valid @RequestBody Report r){
        jobs.visible(repo.get(r.jobId()),p);
        long id=db.insert("INSERT INTO reports(author_id,job_id,reason,detail) VALUES(:author,:job,:reason,:detail)",p("author",p.id(),"job",r.jobId(),"reason",r.reason(),"detail",r.detail().trim()));
        var admins=db.list("SELECT id FROM users WHERE role='ADMIN' AND active=TRUE").stream().map(a->id(a,"id")).toList();
        events.publishEvent(DomainEvent.of("REPORT_CREATED",p.id(),r.jobId(),"Nuevo reporte #"+id,"Revisa el caso desde administracion.",admins));
        return Map.of("id",id,"message","Reporte registrado. No es un canal de atencion de emergencias.");
    }
}
