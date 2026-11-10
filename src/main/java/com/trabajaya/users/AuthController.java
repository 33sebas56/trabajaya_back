package com.trabajaya.users;
import com.trabajaya.shared.*;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private record Window(long start,int count){
    }
    private final Map<String,Window> limits=new ConcurrentHashMap<>();
    public AuthController(AuthService auth){
        this.auth=auth;
    }
    private void limit(HttpServletRequest req,String action){
        long now=System.currentTimeMillis();
        if(limits.size()>10000)limits.entrySet().removeIf(e->now-e.getValue().start()>60000);
        var w=limits.compute(req.getRemoteAddr()+":"+action,(k,v)->v==null||now-v.start()>60000?new Window(now,1):new Window(v.start(),v.count()+1));
        if(w.count()>20)throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,"Demasiados intentos. Espera un minuto.");
    }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) public Object register(@Valid @RequestBody UserRequests.Register r,HttpServletRequest req){
        limit(req,"register");
        return auth.register(r);
    }
    @PostMapping("/login") public Object login(@Valid @RequestBody UserRequests.Login r,HttpServletRequest req){
        limit(req,"login");
        return auth.login(r);
    }
    @PostMapping("/logout") public Object logout(@RequestHeader(value="Authorization",required=false)String header){
        auth.logout(header);
        return Map.of("message","Sesion cerrada.");
    }
    @PostMapping("/forgot-password") public Object forgot(@Valid @RequestBody UserRequests.Forgot r,HttpServletRequest req){
        limit(req,"forgot");
        auth.forgot(r.email());
        return Map.of("message","Si la cuenta existe y la recuperacion esta configurada, recibiras un enlace. En modo local se muestra en la consola del backend.");
    }
    @PostMapping("/reset-password") public Object reset(@Valid @RequestBody UserRequests.Reset r,HttpServletRequest req){
        limit(req,"reset");
        auth.reset(r);
        return Map.of("message","Clave actualizada. Inicia sesion nuevamente.");
    }
}
