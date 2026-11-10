package com.trabajaya.events;
import com.trabajaya.shared.Db;
import static com.trabajaya.shared.Db.p;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
/** Synchronous listener: audit and notifications commit with the business transaction. */
@Component
public class EventRecorder {
    private final Db db;
    public EventRecorder(Db db){
        this.db=db;
    }
    @EventListener public void record(DomainEvent e){
        db.update("INSERT INTO domain_events(id,kind,actor_id,job_id,title,detail) VALUES(:id,:kind,:actor,:job,:title,:detail)",p("id",e.id(),"kind",e.kind(),"actor",e.actorId(),"job",e.jobId(),"title",e.title(),"detail",e.detail()));
        e.recipients().stream().distinct().filter(id->!id.equals(e.actorId())).forEach(id->db.update("INSERT INTO notifications(event_id,user_id,job_id,title,body) VALUES(:event,:user,:job,:title,:body)",p("event",e.id(),"user",id,"job",e.jobId(),"title",e.title(),"body",e.detail())));
    }
}
