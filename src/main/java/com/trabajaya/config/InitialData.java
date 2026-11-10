package com.trabajaya.config;
import com.trabajaya.shared.*;
import com.trabajaya.users.*;
import com.trabajaya.jobs.*;
import static com.trabajaya.shared.Db.*;
import org.springframework.stereotype.Component;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.*;
import java.util.*;
import java.time.*;
@Component
public class InitialData implements ApplicationRunner {
    private static final Logger log=LoggerFactory.getLogger(InitialData.class);
    private final Db db;
    private final AuthService auth;
    private final UserRepository users;
    private final JobService jobs;
    private final PasswordEncoder encoder;
    @Value("${app.seed.enabled:false}")boolean enabled;
    @Value("${app.admin.email:}")String adminEmail;
    @Value("${app.admin.password:}")String adminPassword;
    public InitialData(Db db,AuthService auth,UserRepository users,JobService jobs,PasswordEncoder encoder){
        this.db=db;
        this.auth=auth;
        this.users=users;
        this.jobs=jobs;
        this.encoder=encoder;
    }
    @Override @Transactional public void run(ApplicationArguments args){
        if(!adminEmail.isBlank()&&!adminPassword.isBlank()&&users.byEmail(AuthService.email(adminEmail)).isEmpty()){
            if(adminPassword.length()<12)throw new IllegalArgumentException("ADMIN_PASSWORD debe tener al menos 12 caracteres.");
            db.update("INSERT INTO users(name,email,password_hash,role,phone,city) VALUES('Administrador',:email,:hash,'ADMIN','Sin registrar','Pasto')",p("email",AuthService.email(adminEmail),"hash",encoder.encode(adminPassword)));
        }
        if(!enabled)return;
        if(users.byEmail("cliente@trabajaya.demo").isPresent())return;
        log.warn("MODO DEMOSTRACION: se crearan cuentas y trabajos ficticios. No lo actives en un entorno publico.");
        auth.register(new UserRequests.Register("Laura Martinez","cliente@trabajaya.demo","Demo2026!","CLIENTE","3000000001","Pasto",List.of(),0,15,true));
        auth.register(new UserRequests.Register("Carlos Ruiz","trabajador@trabajaya.demo","Demo2026!","TRABAJADOR","3000000002","Pasto",List.of(1L,2L,5L,6L,10L),5,25,true));
        auth.register(new UserRequests.Register("Ana Torres","ana@trabajaya.demo","Demo2026!","TRABAJADOR","3000000003","Pasto",List.of(1L,3L,5L,7L,9L),3,25,true));
        if(users.byEmail("admin@trabajaya.demo").isEmpty())db.update("INSERT INTO users(name,email,password_hash,role,phone,city) VALUES('Admin Demo','admin@trabajaya.demo',:hash,'ADMIN','3000000000','Pasto')",p("hash",encoder.encode("Demo2026!")));
        long client=id(users.byEmail("cliente@trabajaya.demo").orElseThrow(),"id");
        var principal=new Principal(client,"CLIENTE","Laura Martinez");
        Object[][] entries={
            {
                "Instalar dos lamparas de techo",1L,"Necesito instalar dos lamparas en la sala. Ya tengo los materiales; se requiere herramienta y experiencia.","Centro",70000L,110000L,2,1
            }
            ,
            {
                "Ayuda para descargar un camion",5L,"Busco dos personas para descargar cajas livianas. Hay acceso a nivel de calle y espacio para organizar.","Torobajo",65000L,95000L,3,2
            }
            ,
            {
                "Revisar fuga debajo del lavaplatos",2L,"La conexion del lavaplatos pierde agua. La oferta cubre la visita de diagnostico; materiales y reparacion se acuerdan aparte.","Las Cuadras",40000L,60000L,1,1
            }
            ,
            {
                "Pintar una pared interior",3L,"Una pared de aproximadamente doce metros cuadrados. Suministro pintura y proteccion del piso.","San Ignacio",120000L,180000L,4,1
            }
        }
        ;
        for(int i=0;i<entries.length;i++){
            var e=entries[i];
            jobs.publish(principal,new JobRequests.Publish((String)e[0],(String)e[2],(Long)e[1],"Pasto",(String)e[3],"Direccion ficticia de demostracion "+(i+1),null,null,OffsetDateTime.now().plusDays(2+i).withHour(14).withMinute(0), (Integer)e[6],(Integer)e[7],(Long)e[4],(Long)e[5],10000,10,false,List.of()));
        }
    }
}
