package com.trabajaya.users;
import com.trabajaya.shared.*;
import static com.trabajaya.shared.Db.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@RestController @RequestMapping("/api")
public class ProfileController {
    private final UserRepository users;
    private final Db db;
    public ProfileController(UserRepository users,Db db){
        this.users=users;
        this.db=db;
    }
    @GetMapping("/dashboard") public Object dashboard(@AuthenticationPrincipal Principal principal){
        long uid=principal.id();
        var args=p("id",uid);
        var out=new LinkedHashMap<String,Object>();
        if(principal.is("TRABAJADOR")){
            out.put("active",db.count("SELECT COUNT(*) FROM assignments WHERE worker_id=:id AND status NOT IN ('CONFIRMADO','CANCELADO')",args));
            out.put("completed",db.count("SELECT COUNT(*) FROM assignments WHERE worker_id=:id AND status='CONFIRMADO'",args));
        }
        else{
            out.put("active",db.count("SELECT COUNT(*) FROM jobs WHERE client_id=:id AND status NOT IN ('CANCELADO','COMPLETADO','VENCIDO')",args));
            out.put("completed",db.count("SELECT COUNT(*) FROM jobs WHERE client_id=:id AND status='COMPLETADO'",args));
        }
        var rep=db.require("SELECT COUNT(*) AS total,AVG(CAST(score AS DOUBLE PRECISION)) AS average FROM reviews WHERE target_id=:id",args);
        out.put("rating",rep.get("average"));
        out.put("reviews",rep.get("total"));
        return out;
    }
    @GetMapping("/me") public Object me(@AuthenticationPrincipal Principal p){
        return users.profile(p.id(),true);
    }
    @GetMapping("/users/{id}") public Object publicProfile(@PathVariable long id){
        var user=users.byId(id);
        if(!bool(user,"active"))throw ApiException.missing();
        return users.profile(id,false);
    }
    @PutMapping("/me") @Transactional public Object update(@AuthenticationPrincipal Principal p,@Valid @RequestBody UserRequests.Profile r){
        Geo.validate(r.latitude(),r.longitude());
        if(p.is("TRABAJADOR")&&r.skills().isEmpty())throw ApiException.bad("Selecciona al menos una habilidad.");
        if(r.avatarId()!=null&&!r.avatarId().isBlank()){
            if(db.count("SELECT COUNT(*) FROM media WHERE id=:id AND owner_id=:user AND job_id IS NULL AND assignment_id IS NULL",p("id",r.avatarId(),"user",p.id()))==0)throw ApiException.bad("La imagen de perfil no es tuya o ya esta asociada a otro recurso.");
        }
        db.update("UPDATE users SET name=:name,phone=:phone,city=:city,bio=:bio,experience=:experience,radius_km=:radius,available=:available,latitude=:lat,longitude=:lon,avatar_id=:avatar WHERE id=:id",p("name",r.name().trim(),"phone",r.phone().trim(),"city",r.city().trim(),"bio",r.bio().trim(),"experience",r.experience(),"radius",r.radiusKm(),"available",r.available(),"lat",r.latitude(),"lon",r.longitude(),"avatar",r.avatarId(),"id",p.id()));
        if(p.is("TRABAJADOR"))users.skills(p.id(),r.skills());
        return users.profile(p.id(),true);
    }
}
