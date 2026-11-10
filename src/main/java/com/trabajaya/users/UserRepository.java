package com.trabajaya.users;
import com.trabajaya.shared.*;
import java.util.*;
import org.springframework.stereotype.Repository;
import static com.trabajaya.shared.Db.*;
@Repository
public class UserRepository {
    private final Db db;
    public UserRepository(Db db){
        this.db=db;
    }
    public Optional<Map<String,Object>> byEmail(String email){
        return db.one("SELECT * FROM users WHERE email=:email",p("email",email));
    }
    public Map<String,Object> byId(long id){
        return db.require("SELECT * FROM users WHERE id=:id",p("id",id));
    }
    public List<Long> skills(long id){
        return db.list("SELECT category_id FROM user_skills WHERE user_id=:id",p("id",id)).stream().map(m->id(m,"category_id")).toList();
    }
    public void skills(long id,List<Long> skills){
        if(skills==null||skills.size()>20)throw ApiException.bad("Selecciona hasta 20 habilidades.");
        db.update("DELETE FROM user_skills WHERE user_id=:id",p("id",id));
        for(Long skill:new LinkedHashSet<>(skills)){
            if(skill==null||db.count("SELECT COUNT(*) FROM categories WHERE id=:id AND active=TRUE",p("id",skill))==0)throw ApiException.bad("La habilidad seleccionada no esta activa.");
            db.update("INSERT INTO user_skills(user_id,category_id) VALUES(:user,:category)",p("user",id,"category",skill));
        }
    }
    public Map<String,Object> profile(long id,boolean self){
        var raw=byId(id);
        var out=new LinkedHashMap<String,Object>();
        for(String key:List.of("id","name","role","city","bio","experience","available","avatar_id","created_at"))out.put(key,raw.get(key));
        if(self)for(String key:List.of("email","phone","radius_km","latitude","longitude","active"))out.put(key,raw.get(key));
        out.put("skills",skills(id));
        out.put("reputation",db.require("SELECT COUNT(*) AS total, AVG(CAST(score AS DOUBLE PRECISION)) AS average FROM reviews WHERE target_id=:id",p("id",id)));
        out.put("reviews",db.list("SELECT r.score,r.comment,r.created_at,u.name AS author_name FROM reviews r JOIN users u ON u.id=r.author_id WHERE r.target_id=:id ORDER BY r.id DESC LIMIT 30",p("id",id)));
        return out;
    }
}
