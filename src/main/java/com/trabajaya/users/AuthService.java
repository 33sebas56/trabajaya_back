package com.trabajaya.users;
import com.trabajaya.shared.*;
import static com.trabajaya.shared.Db.*;
import java.util.*;
import java.time.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.slf4j.*;
@Service
public class AuthService {
    private final Db db;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JavaMailSender mail;
    private static final Logger log=LoggerFactory.getLogger(AuthService.class);
    private final SecureRandom random=new SecureRandom();
    @Value("${app.auth.session-hours}") int sessionHours;
    @Value("${app.reset.console:false}") boolean consoleReset;
    @Value("${app.mail.enabled:false}") boolean mailEnabled;
    @Value("${app.mail.from}") String mailFrom;
    @Value("${app.frontend-url}") String frontUrl;
    public AuthService(Db db,UserRepository users,PasswordEncoder encoder,JavaMailSender mail){
        this.db=db;
        this.users=users;
        this.encoder=encoder;
        this.mail=mail;
    }
    public static String hash(String value){
        try{
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        }
        catch(NoSuchAlgorithmException e){
            throw new IllegalStateException(e);
        }
    }
    private String token(){
        byte[] bytes=new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    public static String email(String value){
        return value.trim().toLowerCase(Locale.ROOT);
    }
    private void password(String p){
        if(p.length()<8||p.getBytes(StandardCharsets.UTF_8).length>72||!p.matches("(?s).*[A-Za-z].*")||!p.matches("(?s).*[0-9].*"))throw ApiException.bad("La clave debe tener de 8 a 72 bytes e incluir letras y numeros.");
    }
    @Transactional public Map<String,Object> register(UserRequests.Register r){
        password(r.password());
        if(users.byEmail(email(r.email())).isPresent())throw ApiException.conflict("No se puede registrar ese correo. Intenta iniciar sesion o recupera tu acceso.");
        if(r.role().equals("TRABAJADOR")&&r.skills().isEmpty())throw ApiException.bad("Selecciona al menos una habilidad.");
        long id=db.insert("INSERT INTO users(name,email,password_hash,role,phone,city,experience,radius_km) VALUES(:name,:email,:hash,:role,:phone,:city,:experience,:radius)",p("name",r.name().trim(),"email",email(r.email()),"hash",encoder.encode(r.password()),"role",r.role(),"phone",r.phone().trim(),"city",r.city().trim(),"experience",r.experience(),"radius",r.radiusKm()));
        if(r.role().equals("TRABAJADOR"))users.skills(id,r.skills());
        return session(id);
    }
    @Transactional public Map<String,Object> login(UserRequests.Login r){
        var u=users.byEmail(email(r.email())).orElse(null);
        if(u==null||!encoder.matches(r.password(),text(u,"password_hash"))||!bool(u,"active"))throw new ApiException(HttpStatus.UNAUTHORIZED,"Correo o clave incorrectos, o cuenta no disponible.");
        return session(id(u,"id"));
    }
    private Map<String,Object> session(long id){
        String token=token();
        db.update("INSERT INTO auth_sessions(token_hash,user_id,expires_at) VALUES(:hash,:user,:expires)",p("hash",hash(token),"user",id,"expires",OffsetDateTime.now().plusHours(sessionHours)));
        return Map.of("token",token,"user",users.profile(id,true));
    }
    public Optional<Principal> authenticate(String raw){
        if(raw==null||raw.length()>100)return Optional.empty();
        return db.one("SELECT u.id,u.role,u.name FROM auth_sessions s JOIN users u ON u.id=s.user_id WHERE s.token_hash=:hash AND s.expires_at>CURRENT_TIMESTAMP AND u.active=TRUE",p("hash",hash(raw))).map(u->new Principal(id(u,"id"),text(u,"role"),text(u,"name")));
    }
    public void logout(String header){
        if(header!=null&&header.startsWith("Bearer "))db.update("DELETE FROM auth_sessions WHERE token_hash=:hash",p("hash",hash(header.substring(7))));
    }
    @Transactional public void forgot(String email){
        var u=users.byEmail(email(email)).orElse(null);
        if(u==null||!bool(u,"active"))return;
        if(!consoleReset&&!mailEnabled){
            log.warn("Password recovery requested but SMTP is not configured.");
            return;
        }
        long uid=id(u,"id");
        String raw=token();
        db.update("DELETE FROM password_resets WHERE user_id=:id",p("id",uid));
        db.update("INSERT INTO password_resets(token_hash,user_id,expires_at) VALUES(:hash,:user,:expires)",p("hash",hash(raw),"user",uid,"expires",OffsetDateTime.now().plusMinutes(20)));
        String link=frontUrl+"/#/restablecer?token="+raw;
        if(consoleReset)log.info("SOLO DESARROLLO - Recuperacion para {}: {}",text(u,"email"),link);
        if(mailEnabled){
            try{
                var message=new SimpleMailMessage();
                message.setFrom(mailFrom);
                message.setTo(text(u,"email"));
                message.setSubject("Restablecer acceso a TrabajaYA");
                message.setText("Este enlace vence en 20 minutos y solo puede usarse una vez:\n"+link);
                mail.send(message);
            }
            catch(Exception ex){
                log.warn("No se pudo enviar el correo de recuperacion. Revisa SMTP.");
            }
        }
    }
    @Transactional public void reset(UserRequests.Reset r){
        password(r.password());
        String hash=hash(r.token());
        var reset=db.one("SELECT * FROM password_resets WHERE token_hash=:hash AND used=FALSE AND expires_at>CURRENT_TIMESTAMP FOR UPDATE",p("hash",hash)).orElseThrow(()->ApiException.bad("El enlace es invalido o ha vencido."));
        long uid=id(reset,"user_id");
        db.update("UPDATE users SET password_hash=:hash WHERE id=:id",p("hash",encoder.encode(r.password()),"id",uid));
        db.update("UPDATE password_resets SET used=TRUE WHERE token_hash=:hash",p("hash",hash));
        db.update("DELETE FROM auth_sessions WHERE user_id=:id",p("id",uid));
    }
}
