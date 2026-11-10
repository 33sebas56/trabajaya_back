package com.trabajaya.jobs;
import com.trabajaya.shared.*;
import com.trabajaya.users.UserRepository;
import com.trabajaya.events.DomainEvent;
import static com.trabajaya.shared.Db.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import java.time.*;
import java.util.*;
@Service
public class JobService {
    private final Db db;
    private final JobRepository repo;
    private final UserRepository users;
    private final ApplicationEventPublisher events;
    public JobService(Db db,JobRepository repo,UserRepository users,ApplicationEventPublisher events){
        this.db=db;
        this.repo=repo;
        this.users=users;
        this.events=events;
    }
    private boolean owner(Map<String,Object> j,Principal p){
        return id(j,"client_id")==p.id();
    }
    private void owned(Map<String,Object> j,Principal p){
        p.require("CLIENTE");
        if(!owner(j,p))throw ApiException.forbidden();
    }
    private void activeCategory(long id){
        if(db.count("SELECT COUNT(*) FROM categories WHERE id=:id AND active=TRUE",p("id",id))==0)throw ApiException.bad("La categoria no esta disponible.");
    }
    private void editable(Map<String,Object> j){
        if(!text(j,"status").equals("PUBLICADO")||bool(j,"hidden")||repo.occupied(id(j,"id"))>0)throw ApiException.conflict("Solo puedes editar antes de que alguien acepte.");
    }
    public void visible(Map<String,Object> j,Principal p){
        boolean involved=owner(j,p)||repo.isAssigned(id(j,"id"),p.id());
        if(bool(j,"hidden")&&!p.is("ADMIN")&&!involved)throw ApiException.missing();
        if(!involved&&!p.is("ADMIN")&&(!text(j,"status").equals("PUBLICADO")||time(j,"scheduled_at").isBefore(OffsetDateTime.now())))throw ApiException.missing();
    }
    public Map<String,Object> detail(long id,Principal p){
        return present(repo.get(id),p,true);
    }
    private Map<String,Object> present(Map<String,Object> j,Principal p,boolean full){
        long job=id(j,"id");
        visible(j,p);
        boolean privateAccess=owner(j,p)||p.is("ADMIN")||repo.isAssigned(job,p.id());
        var out=new LinkedHashMap<String,Object>();
        for(String key:List.of("id","client_id","client_name","category_id","category_name","title","description","city","zone","scheduled_at","duration_hours","workers_needed","current_price","status","hidden","created_at"))out.put(key,j.get(key));
        out.put("occupied",repo.occupied(job));
        out.put("images",db.list("SELECT id FROM media WHERE job_id=:id ORDER BY created_at",p("id",job)).stream().map(m->m.get("id")).toList());
        if(privateAccess){
            for(String key:List.of("address","latitude","longitude","cancel_reason"))out.put(key,j.get(key));
        }
        var me=users.byId(p.id());
        Double lat=decimal(j,"latitude"),lon=decimal(j,"longitude"),myLat=decimal(me,"latitude"),myLon=decimal(me,"longitude");
        out.put("distance_km",lat!=null&&myLat!=null?Math.max(1,Math.round(Geo.km(myLat,myLon,lat,lon))):null);
        if(owner(j,p)||p.is("ADMIN"))for(String key:List.of("initial_price","max_price","increment_amount","interval_minutes","auto_increase","next_increase_at"))out.put(key,j.get(key));
        if(full){
            var assignments=repo.assignments(job);
            out.put("assignments",assignments.stream().filter(a->owner(j,p)||p.is("ADMIN")||id(a,"worker_id")==p.id()).map(a->{
                var copy=new LinkedHashMap<>(a);copy.put("reviews",db.list("SELECT id,author_id,target_id,score,comment FROM reviews WHERE assignment_id=:id",p("id",id(a,"id"))));return copy;
            }
            ).toList());
            out.put("price_history",db.list("SELECT amount,origin,created_at FROM price_history WHERE job_id=:id ORDER BY id",p("id",job)));
            if(privateAccess)out.put("timeline",db.list("SELECT e.id,e.kind,e.detail,e.created_at,u.name AS actor_name FROM job_events e LEFT JOIN users u ON u.id=e.actor_id WHERE e.job_id=:id ORDER BY e.id DESC LIMIT 100",p("id",job)));
            out.put("client_reputation",db.require("SELECT COUNT(*) AS total,AVG(CAST(score AS DOUBLE PRECISION)) AS average FROM reviews WHERE target_id=:id",p("id",id(j,"client_id"))));
        }
        return out;
    }
    public Map<String,Object> list(Principal p,boolean mine,Long category,Long minimum,String city,String date,int page,int size){
        page=Math.max(0,page);
        size=Math.max(1,Math.min(size,50));
        var args=p("me",p.id());
        String where="";
        if(mine){
            where=p.is("CLIENTE")?"j.client_id=:me":p.is("TRABAJADOR")?"EXISTS(SELECT 1 FROM assignments a WHERE a.job_id=j.id AND a.worker_id=:me AND a.status<>'CANCELADO')":"1=1";
        }
        else {
            where="j.status='PUBLICADO' AND j.hidden=FALSE AND j.scheduled_at>CURRENT_TIMESTAMP AND u.active=TRUE";
            if(p.is("TRABAJADOR")){
                where+=" AND EXISTS(SELECT 1 FROM user_skills s WHERE s.user_id=:me AND s.category_id=j.category_id)";
                var person=users.byId(p.id());
                args.put("myCity",text(person,"city"));
                if(person.get("latitude")!=null){
                    args.put("myLat",decimal(person,"latitude"));
                    args.put("myLon",decimal(person,"longitude"));
                    args.put("radius",id(person,"radius_km"));
                    where+=" AND ((j.latitude IS NULL AND LOWER(j.city)=LOWER(:myCity)) OR (j.latitude IS NOT NULL AND 6371 * ACOS(LEAST(1.0,GREATEST(-1.0,SIN(RADIANS(CAST(:myLat AS DOUBLE PRECISION)))*SIN(RADIANS(j.latitude))+COS(RADIANS(CAST(:myLat AS DOUBLE PRECISION)))*COS(RADIANS(j.latitude))*COS(RADIANS(j.longitude-CAST(:myLon AS DOUBLE PRECISION)))))) <= :radius))";
                }
                else where+=" AND LOWER(j.city)=LOWER(:myCity)";
            }
        }
        if(category!=null){
            where+=" AND j.category_id=:category";
            args.put("category",category);
        }
        if(minimum!=null){
            where+=" AND j.current_price>=:minimum";
            args.put("minimum",minimum);
        }
        if(city!=null&&!city.isBlank()){
            where+=" AND LOWER(j.city)=LOWER(:city)";
            args.put("city",city.trim());
        }
        if(date!=null&&!date.isBlank()){
            try {
                var day=LocalDate.parse(date);
                where+=" AND j.scheduled_at>=:daystart AND j.scheduled_at<:dayend";
                args.put("daystart",day.atStartOfDay().atOffset(ZoneOffset.ofHours(-5)));
                args.put("dayend",day.plusDays(1).atStartOfDay().atOffset(ZoneOffset.ofHours(-5)));
            }
            catch(Exception e){
                throw ApiException.bad("La fecha debe tener formato AAAA-MM-DD.");
            }
        }
        // Radius filtering happens in SQL before COUNT and pagination; exact coordinates stay private.
        long total=db.count("SELECT COUNT(*) FROM jobs j JOIN users u ON u.id=j.client_id WHERE "+where,args);
        args.put("limit",size);
        args.put("offset",page*size);
        var items=db.list("SELECT j.*,c.name AS category_name,u.name AS client_name FROM jobs j JOIN categories c ON c.id=j.category_id JOIN users u ON u.id=j.client_id WHERE "+where+" ORDER BY j.id DESC LIMIT :limit OFFSET :offset",args);
        return Map.of("items",items.stream().map(j->present(j,p,false)).toList(),"total",total,"page",page,"size",size);
    }
    private void attach(long job,long owner,List<String> ids){
        for(String media:new LinkedHashSet<>(ids)){
            var m=db.require("SELECT * FROM media WHERE id=:id FOR UPDATE",p("id",media));
            if(id(m,"owner_id")!=owner||m.get("assignment_id")!=null||(m.get("job_id")!=null&&id(m,"job_id")!=job)||db.count("SELECT COUNT(*) FROM users WHERE avatar_id=:id",p("id",media))>0)throw ApiException.forbidden();
        }
        db.update("UPDATE media SET job_id=NULL WHERE job_id=:id",p("id",job));
        for(String media:new LinkedHashSet<>(ids))db.update("UPDATE media SET job_id=:job WHERE id=:id",p("job",job,"id",media));
    }
    @Transactional public Object publish(Principal p,JobRequests.Publish r){
        p.require("CLIENTE");
        activeCategory(r.categoryId());
        Geo.validate(r.latitude(),r.longitude());
        if(r.maxPrice()<r.initialPrice())throw ApiException.bad("El maximo no puede ser menor al precio inicial.");
        long id=db.insert("INSERT INTO jobs(client_id,category_id,title,description,city,zone,address,latitude,longitude,scheduled_at,duration_hours,workers_needed,initial_price,current_price,max_price,increment_amount,interval_minutes,auto_increase,next_increase_at) VALUES(:client,:category,:title,:description,:city,:zone,:address,:lat,:lon,:scheduled,:duration,:workers,:initial,:initial,:max,:increment,:interval,:auto,:next)",p("client",p.id(),"category",r.categoryId(),"title",r.title().trim(),"description",r.description().trim(),"city",r.city().trim(),"zone",r.zone().trim(),"address",r.address().trim(),"lat",r.latitude(),"lon",r.longitude(),"scheduled",r.scheduledAt(),"duration",r.durationHours(),"workers",r.workersNeeded(),"initial",r.initialPrice(),"max",r.maxPrice(),"increment",r.incrementAmount(),"interval",r.intervalMinutes(),"auto",r.autoIncrease(),"next",OffsetDateTime.now().plusMinutes(r.intervalMinutes())));
        attach(id,p.id(),r.mediaIds());
        db.update("INSERT INTO price_history(job_id,amount,origin) VALUES(:job,:price,'PUBLICACION')",p("job",id,"price",r.initialPrice()));
        repo.event(id,null,p.id(),"PUBLICADO","Trabajo publicado.");
        events.publishEvent(DomainEvent.of("JOB_PUBLISHED",p.id(),id,"Nueva oportunidad: "+r.title(),"Hay un trabajo compatible con tus habilidades.",matching(repo.get(id))));
        return detail(id,p);
    }
    @Transactional public Object edit(long id,Principal p,JobRequests.Edit r){
        var j=repo.lock(id);
        owned(j,p);
        editable(j);
        activeCategory(r.categoryId());
        Geo.validate(r.latitude(),r.longitude());
        db.update("UPDATE jobs SET title=:title,description=:description,category_id=:category,city=:city,zone=:zone,address=:address,latitude=:lat,longitude=:lon,scheduled_at=:scheduled,duration_hours=:duration,workers_needed=:workers WHERE id=:id",p("id",id,"title",r.title().trim(),"description",r.description().trim(),"category",r.categoryId(),"city",r.city().trim(),"zone",r.zone().trim(),"address",r.address().trim(),"lat",r.latitude(),"lon",r.longitude(),"scheduled",r.scheduledAt(),"duration",r.durationHours(),"workers",r.workersNeeded()));
        attach(id,p.id(),r.mediaIds());
        repo.event(id,null,p.id(),"EDITADO","El cliente actualizo la publicacion.");
        return detail(id,p);
    }
    @Transactional public Object increase(long id,Principal p,long amount){
        var j=repo.lock(id);
        owned(j,p);
        increaseLocked(j,amount,p.id());
        return detail(id,p);
    }
    private void increaseLocked(Map<String,Object> j,long amount,Long actor){
        long id=id(j,"id");
        if(!text(j,"status").equals("PUBLICADO")||bool(j,"hidden")||time(j,"scheduled_at").isBefore(OffsetDateTime.now())||repo.occupied(id)>=id(j,"workers_needed"))throw ApiException.conflict("El trabajo ya no admite aumentos.");
        long current=id(j,"current_price"),max=id(j,"max_price");
        if(current>=max)throw ApiException.conflict("Ya alcanzaste el presupuesto maximo.");
        long next=JobRules.increasedPrice(current,amount,max);
        db.update("UPDATE jobs SET current_price=:price,next_increase_at=:next WHERE id=:id",p("price",next,"next",OffsetDateTime.now().plusMinutes(id(j,"interval_minutes")),"id",id));
        db.update("INSERT INTO price_history(job_id,amount,origin) VALUES(:job,:price,:origin)",p("job",id,"price",next,"origin",actor==null?"AUTOMATICO":"MANUAL"));
        repo.event(id,null,actor,"PRECIO","La oferta aumento a $"+next+" por persona. Los cupos aceptados conservan su valor.");
        events.publishEvent(DomainEvent.of("PRICE_INCREASED",actor,id,"Aumento de oferta",text(j,"title")+": $"+next+" por cupo disponible.",matching(j)));
    }
    @Transactional public void tick(long id){
        var j=repo.lock(id);
        if(!text(j,"status").equals("PUBLICADO")||bool(j,"hidden"))return;
        if(time(j,"scheduled_at").isBefore(OffsetDateTime.now())){
            if(repo.occupied(id)==0){
                db.update("UPDATE jobs SET status='VENCIDO' WHERE id=:id",p("id",id));
                repo.event(id,null,null,"VENCIDO","La fecha del trabajo paso sin asignaciones.");
            }
            return;
        }
        if(bool(j,"auto_increase")&&id(j,"current_price")<id(j,"max_price")&&time(j,"next_increase_at")!=null&&!time(j,"next_increase_at").isAfter(OffsetDateTime.now()))increaseLocked(j,id(j,"increment_amount"),null);
    }
    @Transactional public Object accept(long id,Principal p,long expected){
        p.require("TRABAJADOR");
        var j=repo.lock(id);
        var me=users.byId(p.id());
        if(!bool(users.byId(id(j,"client_id")),"active"))throw ApiException.conflict("El cliente no esta disponible.");
        if(owner(j,p)||bool(j,"hidden")||!text(j,"status").equals("PUBLICADO")||time(j,"scheduled_at").isBefore(OffsetDateTime.now()))throw ApiException.conflict("El trabajo ya no esta disponible.");
        if(!bool(me,"available")||!users.skills(p.id()).contains(id(j,"category_id")))throw ApiException.bad("Activa tu disponibilidad y verifica tus habilidades.");
        if(!within(j,me))throw ApiException.bad("El trabajo esta fuera de tu zona o radio configurado.");
        if(id(j,"current_price")!=expected)throw ApiException.conflict("El precio cambio. Actualiza la publicacion antes de aceptar.");
        if(repo.occupied(id)>=id(j,"workers_needed"))throw ApiException.conflict("Ya no quedan cupos.");
        var prior=db.one("SELECT * FROM assignments WHERE job_id=:job AND worker_id=:user",p("job",id,"user",p.id()));
        long assignment;
        if(prior.isPresent()){
            var old=prior.get();
            if(!text(old,"status").equals("CANCELADO"))throw ApiException.conflict("Ya aceptaste este trabajo.");
            assignment=id(old,"id");
            db.update("UPDATE assignments SET agreed_price=:price,status='ACEPTADO',accepted_at=CURRENT_TIMESTAMP,completed_at=NULL WHERE id=:id",p("price",expected,"id",assignment));
        }
        else assignment=db.insert("INSERT INTO assignments(job_id,worker_id,agreed_price) VALUES(:job,:worker,:price)",p("job",id,"worker",p.id(),"price",expected));
        repo.sync(id);
        repo.event(id,assignment,p.id(),"ACEPTADO",p.name()+" acepto un cupo por $"+expected+".");
        events.publishEvent(DomainEvent.of("JOB_ACCEPTED",p.id(),id,"Un trabajador acepto",p.name()+" acepto: "+text(j,"title"),List.of(id(j,"client_id"))));
        return detail(id,p);
    }
    @Transactional public Object cancel(long id,Principal p,String reason){
        var j=repo.lock(id);
        owned(j,p);
        if(Set.of("CANCELADO","COMPLETADO","VENCIDO").contains(text(j,"status")))throw ApiException.conflict("El trabajo ya esta cerrado.");
        if(repo.assignments(id).stream().anyMatch(a->!text(a,"status").equals("CANCELADO")&&!JobRules.cancellable(text(a,"status"))))throw ApiException.conflict("El trabajo ya inicio. Abre un reporte para resolverlo.");
        var recipients=repo.related(id);
        db.update("UPDATE jobs SET status='CANCELADO',cancel_reason=:reason WHERE id=:id",p("id",id,"reason",reason));
        db.update("UPDATE assignments SET status='CANCELADO' WHERE job_id=:id",p("id",id));
        repo.event(id,null,p.id(),"CANCELADO",reason);
        events.publishEvent(DomainEvent.of("JOB_CANCELLED",p.id(),id,"Trabajo cancelado",reason,recipients));
        return detail(id,p);
    }
    @Transactional public Object assignmentState(long aid,Principal p,String next){
        var initial=repo.assignment(aid);
        long jid=id(initial,"job_id");
        var j=repo.lock(jid);
        var a=repo.assignment(aid);
        if(bool(j,"hidden")||Set.of("CANCELADO","COMPLETADO").contains(text(j,"status")))throw ApiException.conflict("Este trabajo ya esta cerrado o moderado.");
        boolean confirmation=next.equals("CONFIRMADO");
        if(confirmation){
            if(!owner(j,p))throw ApiException.forbidden();
            if(!text(a,"status").equals("TERMINADO"))throw ApiException.conflict("El trabajador aun no ha terminado.");
        }
        else {
            if(!p.is("TRABAJADOR")||id(a,"worker_id")!=p.id())throw ApiException.forbidden();
            if(next.equals("CANCELADO")){
                if(!JobRules.cancellable(text(a,"status"))||repo.assignments(jid).stream().anyMatch(x->Set.of("INICIADO","TERMINADO","CONFIRMADO").contains(text(x,"status"))))throw ApiException.conflict("No puedes retirarte cuando la ejecucion ya comenzo. Abre un reporte.");
            }
            else if(!JobRules.canAdvance(text(a,"status"),next))throw ApiException.conflict("El cambio de estado no esta permitido.");
            if(next.equals("INICIADO")&&repo.occupied(jid)<id(j,"workers_needed"))throw ApiException.conflict("Primero deben completarse los cupos requeridos.");
        }
        db.update("UPDATE assignments SET status=:status,completed_at=:completed WHERE id=:id",p("status",next,"completed",confirmation?OffsetDateTime.now():null,"id",aid));
        repo.sync(jid);
        repo.event(jid,aid,p.id(),next,p.name()+" actualizo su participacion: "+next);
        events.publishEvent(DomainEvent.of("ASSIGNMENT_UPDATED",p.id(),jid,"Avance del trabajo",text(j,"title")+": "+next,repo.related(jid)));
        return confirmation||!next.equals("CANCELADO")?detail(jid,p):Map.of("message","Cupo liberado.");
    }
    public boolean within(Map<String,Object> job,Map<String,Object> worker){
        Double a=decimal(worker,"latitude"),b=decimal(job,"latitude");
        if(a!=null&&b!=null)return Geo.km(a,decimal(worker,"longitude"),b,decimal(job,"longitude"))<=id(worker,"radius_km");
        return text(worker,"city").trim().equalsIgnoreCase(text(job,"city").trim());
    }
    private List<Long> matching(Map<String,Object> job){
        return db.list("SELECT u.* FROM users u JOIN user_skills s ON s.user_id=u.id WHERE u.role='TRABAJADOR' AND u.active=TRUE AND u.available=TRUE AND s.category_id=:category",p("category",id(job,"category_id"))).stream().filter(u->within(job,u)).map(u->id(u,"id")).toList();
    }
}
