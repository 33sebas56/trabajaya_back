package com.trabajaya.jobs;
import com.trabajaya.shared.*;
import static com.trabajaya.shared.Db.*;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public class JobRepository {
    private final Db db;
    public JobRepository(Db db){
        this.db=db;
    }
    public Map<String,Object> get(long id){
        return db.require("SELECT j.*,c.name AS category_name,u.name AS client_name FROM jobs j JOIN categories c ON c.id=j.category_id JOIN users u ON u.id=j.client_id WHERE j.id=:id",p("id",id));
    }
    public Map<String,Object> lock(long id){
        return db.require("SELECT * FROM jobs WHERE id=:id FOR UPDATE",p("id",id));
    }
    public List<Map<String,Object>> assignments(long id){
        return db.list("SELECT a.*,u.name AS worker_name,u.avatar_id AS worker_avatar FROM assignments a JOIN users u ON u.id=a.worker_id WHERE a.job_id=:id ORDER BY a.id",p("id",id));
    }
    public long occupied(long id){
        return db.count("SELECT COUNT(*) FROM assignments WHERE job_id=:id AND status<>'CANCELADO'",p("id",id));
    }
    public Map<String,Object> assignment(long id){
        return db.require("SELECT a.*,j.client_id,j.title,j.status AS job_status,j.hidden FROM assignments a JOIN jobs j ON j.id=a.job_id WHERE a.id=:id",p("id",id));
    }
    public boolean isAssigned(long job,long user){
        return db.count("SELECT COUNT(*) FROM assignments WHERE job_id=:job AND worker_id=:user AND status<>'CANCELADO'",p("job",job,"user",user))>0;
    }
    public boolean participant(Map<String,Object> a,Principal user){
        return id(a,"client_id")==user.id()||id(a,"worker_id")==user.id();
    }
    public void event(long job,Long assignment,Long actor,String kind,String detail){
        db.update("INSERT INTO job_events(job_id,assignment_id,actor_id,kind,detail) VALUES(:job,:assignment,:actor,:kind,:detail)",p("job",job,"assignment",assignment,"actor",actor,"kind",kind,"detail",detail));
    }
    public void sync(long job){
        var j=get(job);
        String next=JobRules.aggregate(assignments(job).stream().map(a->text(a,"status")).toList(),(int)id(j,"workers_needed"));
        db.update("UPDATE jobs SET status=:status WHERE id=:id",p("status",next,"id",job));
    }
    public List<Long> related(long job){
        var ids=new ArrayList<Long>();
        ids.add(id(get(job),"client_id"));
        assignments(job).stream().filter(a->!text(a,"status").equals("CANCELADO")).forEach(a->ids.add(id(a,"worker_id")));
        return ids;
    }
}
