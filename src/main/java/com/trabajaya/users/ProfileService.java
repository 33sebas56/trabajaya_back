package com.trabajaya.users;

import com.trabajaya.shared.*;
import static com.trabajaya.shared.Db.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class ProfileService {
    private final UserRepository users;
    private final Db db;

    public ProfileService(UserRepository users, Db db) {
        this.users = users;
        this.db = db;
    }

    public Object dashboard(Principal principal) {
        long uid = principal.id();
        var args = p("id", uid);
        var out = new LinkedHashMap<String, Object>();
        if (principal.is("TRABAJADOR")) {
            out.put("active", db.count("SELECT COUNT(*) FROM assignments WHERE worker_id=:id AND status NOT IN ('CONFIRMADO','CANCELADO')", args));
            out.put("completed", db.count("SELECT COUNT(*) FROM assignments WHERE worker_id=:id AND status='CONFIRMADO'", args));
        } else {
            out.put("active", db.count("SELECT COUNT(*) FROM jobs WHERE client_id=:id AND status NOT IN ('CANCELADO','COMPLETADO','VENCIDO')", args));
            out.put("completed", db.count("SELECT COUNT(*) FROM jobs WHERE client_id=:id AND status='COMPLETADO'", args));
        }
        var rep = db.require("SELECT COUNT(*) AS total,AVG(CAST(score AS DOUBLE PRECISION)) AS average FROM reviews WHERE target_id=:id", args);
        out.put("rating", rep.get("average"));
        out.put("reviews", rep.get("total"));
        return out;
    }

    public Object me(Principal p) {
        return users.profile(p.id(), true);
    }

    public Object publicProfile(long id) {
        var user = users.byId(id);
        if (!bool(user, "active")) throw ApiException.missing();
        return users.profile(id, false);
    }

    @Transactional
    public Object update(Principal p, UserRequests.Profile r) {
        Geo.validate(r.latitude(), r.longitude());
        if (p.is("TRABAJADOR") && r.skills().isEmpty()) throw ApiException.bad("Selecciona al menos una habilidad.");
        db.require("SELECT id FROM users WHERE id=:id FOR UPDATE", p("id", p.id()));
        String avatar = r.avatarId() == null || r.avatarId().isBlank() ? null : r.avatarId();
        if (avatar != null) {
            var image = db.require("SELECT * FROM media WHERE id=:id FOR UPDATE", p("id", avatar));
            if (id(image, "owner_id") != p.id() || image.get("job_id") != null || image.get("assignment_id") != null) {
                throw ApiException.bad("La imagen de perfil no es tuya o ya esta asociada a otro recurso.");
            }
        }
        db.update("UPDATE users SET name=:name,phone=:phone,city=:city,bio=:bio,experience=:experience,radius_km=:radius,available=:available,latitude=:lat,longitude=:lon,avatar_id=:avatar WHERE id=:id", p("name", r.name().trim(), "phone", r.phone().trim(), "city", r.city().trim(), "bio", r.bio().trim(), "experience", r.experience(), "radius", r.radiusKm(), "available", r.available(), "lat", r.latitude(), "lon", r.longitude(), "avatar", avatar, "id", p.id()));
        if (p.is("TRABAJADOR")) users.skills(p.id(), r.skills());
        return users.profile(p.id(), true);
    }
}
