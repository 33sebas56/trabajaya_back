package com.trabajaya.config;
import com.trabajaya.shared.Db;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import java.util.Map;
@RestController @RequestMapping("/api")
public class PublicController {
    private final Db db;
    @Value("${app.storage.provider}")String storage;
    @Value("${app.seed.enabled:false}")boolean demo;
    public PublicController(Db db){
        this.db=db;
    }
    @GetMapping("/health") public Object health(){
        return Map.of("status","UP","name","TrabajaYA","storage",storage,"demo",demo);
    }
    @GetMapping("/categories") public Object categories(){
        return db.list("SELECT id,name FROM categories WHERE active=TRUE ORDER BY name");
    }
}
