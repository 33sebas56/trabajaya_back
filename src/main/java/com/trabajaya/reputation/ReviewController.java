package com.trabajaya.reputation;
import com.trabajaya.shared.*;
import com.trabajaya.jobs.JobRepository;
import com.trabajaya.events.DomainEvent;
import static com.trabajaya.shared.Db.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import java.util.*;
@RestController @RequestMapping("/api/assignments/{assignment}/review")
public class ReviewController {
    private final Db db;
    private final JobRepository jobs;
    private final ApplicationEventPublisher events;
    public record Rate(@Min(1) @Max(5) int score,@NotNull @Size(max=1500) String comment){
    }
    public ReviewController(Db db,JobRepository jobs,ApplicationEventPublisher events){
        this.db=db;
        this.jobs=jobs;
        this.events=events;
    }
    @PostMapping @Transactional public Object rate(@PathVariable long assignment,@AuthenticationPrincipal Principal p,@Valid @RequestBody Rate r){
        var a=jobs.assignment(assignment);
        if(!jobs.participant(a,p))throw ApiException.forbidden();
        if(!text(a,"status").equals("CONFIRMADO"))throw ApiException.conflict("La participacion debe estar confirmada para calificar.");
        long target=p.id()==id(a,"client_id")?id(a,"worker_id"):id(a,"client_id");
        long id=db.insert("INSERT INTO reviews(assignment_id,author_id,target_id,score,comment) VALUES(:assignment,:author,:target,:score,:comment)",p("assignment",assignment,"author",p.id(),"target",target,"score",r.score(),"comment",r.comment().trim()));
        events.publishEvent(DomainEvent.of("REVIEW_CREATED",p.id(),id(a,"job_id"),"Recibiste una calificacion","Tu perfil tiene una nueva opinion.",List.of(target)));
        return Map.of("id",id,"message","Calificacion guardada.");
    }
}
