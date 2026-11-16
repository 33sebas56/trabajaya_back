package com.trabajaya;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trabajaya.shared.Db;
import com.trabajaya.jobs.JobService;
import com.trabajaya.users.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static com.trabajaya.shared.Db.p;
/** Real Spring context and Flyway; H2 by default, PostgreSQL in the CI matrix. */
@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc
class FlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired Db db;
    @Autowired JobService jobs;
    record Account(String token,long id,String email){
    }
    private JsonNode request(String token,String verb,String path,Object body,int expected)throws Exception {
        MockHttpServletRequestBuilder b=switch(verb){
            case "POST"->post(path);
            case "PUT"->put(path);
            case "PATCH"->patch(path);
            default->get(path);
        }
        ;
        if(token!=null)b.header("Authorization","Bearer "+token);
        if(body!=null)b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
        var response=mvc.perform(b).andReturn().getResponse();
        String responseText=response.getContentAsString();
        assertEquals(expected,response.getStatus(),()->verb+" "+path+" => "+responseText);
        return json.readTree(response.getContentAsByteArray());
    }
    private Account account(String role)throws Exception {
        String email=UUID.randomUUID()+"@test.invalid";
        var data=new LinkedHashMap<String,Object>();
        data.put("name","Prueba "+role);
        data.put("email",email);
        data.put("password","Prueba2026!");
        data.put("role",role);
        data.put("phone","3000000000");
        data.put("city","Pasto");
        data.put("skills",role.equals("TRABAJADOR")?List.of(1):List.of());
        data.put("experience",2);
        data.put("radiusKm",20);
        data.put("acceptedTerms",true);
        JsonNode r=request(null,"POST","/api/auth/register",data,201);
        return new Account(r.path("token").asText(),r.path("user").path("id").asLong(),email);
    }
    private Map<String,Object> publication(int slots){
        var b=new LinkedHashMap<String,Object>();
        b.put("title","Instalar una lampara de prueba");
        b.put("description","Instalar una lampara con los materiales disponibles y condiciones acordadas.");
        b.put("categoryId",1);
        b.put("city","Pasto");
        b.put("zone","Centro");
        b.put("address","Direccion privada de prueba");
        b.put("scheduledAt",OffsetDateTime.now().plusDays(2));
        b.put("durationHours",2);
        b.put("workersNeeded",slots);
        b.put("initialPrice",60000);
        b.put("maxPrice",100000);
        b.put("incrementAmount",10000);
        b.put("intervalMinutes",10);
        b.put("autoIncrease",false);
        b.put("mediaIds",List.of());
        return b;
    }
    private long publish(Account c,int slots)throws Exception{
        return request(c.token,"POST","/api/jobs",publication(slots),201).path("id").asLong();
    }
    private long accept(Account w,long job,long price)throws Exception{
        return request(w.token,"POST","/api/jobs/"+job+"/accept",Map.of("expectedPrice",price),200).path("assignments").get(0).path("id").asLong();
    }
    @Test void completeFlowHasPermissionsPrivacyHistoryAndMutualReviews()throws Exception {
        Account c=account("CLIENTE"),w=account("TRABAJADOR"),outsider=account("TRABAJADOR");
        long job=publish(c,1);
        JsonNode publicJob=request(w.token,"GET","/api/jobs/"+job,null,200);
        assertFalse(publicJob.has("address"));
        assertFalse(publicJob.has("max_price"));
        request(w.token,"POST","/api/jobs/"+job+"/increase",Map.of("amount",10000),403);
        request(c.token,"POST","/api/jobs/"+job+"/increase",Map.of("amount",10000),200);
        request(w.token,"POST","/api/jobs/"+job+"/accept",Map.of("expectedPrice",60000),409);
        long assignment=accept(w,job,70000);
        assertTrue(request(w.token,"GET","/api/jobs/"+job,null,200).has("address"));
        request(c.token,"POST","/api/jobs/"+job+"/increase",Map.of("amount",10000),409);
        request(outsider.token,"GET","/api/assignments/"+assignment+"/messages",null,403);
        request(w.token,"POST","/api/assignments/"+assignment+"/messages",Map.of("body","Voy con las herramientas."),200);
        assertEquals(1,request(c.token,"GET","/api/assignments/"+assignment+"/messages",null,200).size());
        request(c.token,"POST","/api/assignments/"+assignment+"/messages/read",Map.of(),200);
        assertTrue(request(w.token,"GET","/api/assignments/"+assignment+"/messages/read-status",null,200).path("last_read_id").asLong()>0);
        request(w.token,"POST","/api/assignments/"+assignment+"/status",Map.of("status","TERMINADO"),409);
        for(String state:List.of("EN_CAMINO","EN_LUGAR","INICIADO","TERMINADO"))request(w.token,"POST","/api/assignments/"+assignment+"/status",Map.of("status",state),200);
        request(w.token,"POST","/api/assignments/"+assignment+"/status",Map.of("status","CONFIRMADO"),403);
        request(c.token,"POST","/api/assignments/"+assignment+"/status",Map.of("status","CONFIRMADO"),200);
        assertEquals("COMPLETADO",request(c.token,"GET","/api/jobs/"+job,null,200).path("status").asText());
        request(c.token,"POST","/api/assignments/"+assignment+"/review",Map.of("score",5,"comment","Cumplio el alcance."),200);
        request(c.token,"POST","/api/assignments/"+assignment+"/review",Map.of("score",5,"comment","Duplicada"),409);
        request(w.token,"POST","/api/assignments/"+assignment+"/review",Map.of("score",4,"comment","Buen trato."),200);
        request(w.token,"POST","/api/assignments/"+assignment+"/messages",Map.of("body","No se puede escribir al cerrar."),409);
        assertTrue(request(c.token,"GET","/api/notifications",null,200).path("unread").asLong()>0);
    }
    @Test void twoWorkersCannotTakeLastSlot()throws Exception {
        Account c=account("CLIENTE"),a=account("TRABAJADOR"),b=account("TRABAJADOR");
        long job=publish(c,1);
        var gate=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){
            List<Future<Integer>> results=new ArrayList<>();
            for(Account w:List.of(a,b))results.add(pool.submit(()->{
                gate.await();return mvc.perform(post("/api/jobs/"+job+"/accept").header("Authorization","Bearer "+w.token).contentType(MediaType.APPLICATION_JSON).content("{\"expectedPrice\":60000}")).andReturn().getResponse().getStatus();
            }
            ));
            gate.countDown();
            var codes=new ArrayList<Integer>();
            for(var future:results)codes.add(future.get(20,TimeUnit.SECONDS));
            Collections.sort(codes);
            assertEquals(List.of(200,409),codes);
        }
        assertEquals(1,db.count("SELECT COUNT(*) FROM assignments WHERE job_id=:id",p("id",job)));
    }
    @Test void pricesAreFrozenPerPersonAndAutomaticIncreasesAreCapped()throws Exception {
        Account c=account("CLIENTE"),a=account("TRABAJADOR"),b=account("TRABAJADOR");
        long job=publish(c,2);
        accept(a,job,60000);
        request(c.token,"POST","/api/jobs/"+job+"/increase",Map.of("amount",20000),200);
        accept(b,job,80000);
        var detail=request(c.token,"GET","/api/jobs/"+job,null,200);
        assertEquals(60000,detail.path("assignments").get(0).path("agreed_price").asLong());
        assertEquals(80000,detail.path("assignments").get(1).path("agreed_price").asLong());
        long automatic=publish(c,1);
        db.update("UPDATE jobs SET auto_increase=TRUE,next_increase_at=:past WHERE id=:id",p("id",automatic,"past",OffsetDateTime.now().minusMinutes(1)));
        jobs.tick(automatic);
        assertEquals(70000,request(c.token,"GET","/api/jobs/"+automatic,null,200).path("current_price").asLong());
        request(c.token,"POST","/api/jobs/"+automatic+"/increase",Map.of("amount",9999999),200);
        assertEquals(100000,request(c.token,"GET","/api/jobs/"+automatic,null,200).path("current_price").asLong());
    }
    @Test void mediaIsRealPrivateAndRejectsArbitraryFiles()throws Exception {
        Account c=account("CLIENTE"),other=account("CLIENTE");
        var bytes=new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8,8,BufferedImage.TYPE_INT_RGB),"png",bytes);
        var response=mvc.perform(multipart("/api/media").file(new MockMultipartFile("file","photo.png","image/png",bytes.toByteArray())).header("Authorization","Bearer "+c.token)).andReturn().getResponse();
        assertEquals(201,response.getStatus());
        String id=json.readTree(response.getContentAsByteArray()).path("id").asText();
        assertEquals(200,mvc.perform(get("/api/media/"+id).header("Authorization","Bearer "+c.token)).andReturn().getResponse().getStatus());
        request(other.token,"GET","/api/media/"+id,null,403);
        assertEquals(400,mvc.perform(multipart("/api/media").file(new MockMultipartFile("file","wrong.png","image/png","not an image".getBytes())).header("Authorization","Bearer "+c.token)).andReturn().getResponse().getStatus());
    }
    @Test void logoutRevokesSessionAndAdminEndpointsRejectOrdinaryUsers()throws Exception {
        Account c=account("CLIENTE");
        request(c.token,"GET","/api/admin/summary",null,403);
        request(c.token,"POST","/api/auth/logout",Map.of(),200);
        request(c.token,"GET","/api/me",null,401);
    }
    @Test void sqlRadiusFilterAndPasswordRecoveryWork()throws Exception {
        Account c=account("CLIENTE"),w=account("TRABAJADOR");
        long job=publish(c,1);
        db.update("UPDATE users SET latitude=1.2,longitude=-77.2 WHERE id=:id",p("id",w.id));
        db.update("UPDATE jobs SET latitude=1.21,longitude=-77.21 WHERE id=:id",p("id",job));
        assertTrue(request(w.token,"GET","/api/jobs?city=Pasto",null,200).path("total").asLong()>0);
        String raw=UUID.randomUUID().toString();
        db.update("INSERT INTO password_resets(token_hash,user_id,expires_at) VALUES(:hash,:id,:expiry)",p("hash",AuthService.hash(raw),"id",w.id,"expiry",OffsetDateTime.now().plusMinutes(20)));
        request(null,"POST","/api/auth/reset-password",Map.of("token",raw,"password","NuevaClave2026!"),200);
        request(w.token,"GET","/api/me",null,401);
        request(null,"POST","/api/auth/reset-password",Map.of("token",raw,"password","OtraClave2026!"),400);
    }

    @Test void withdrawnWorkerKeepsHistoryButLosesExactAddress() throws Exception {
        Account client = account("CLIENTE"), worker = account("TRABAJADOR");
        long job = publish(client, 1);
        long assignment = accept(worker, job, 60000);
        request(worker.token, "POST", "/api/assignments/" + assignment + "/status", Map.of("status", "CANCELADO"), 200);
        JsonNode detail = request(worker.token, "GET", "/api/jobs/" + job, null, 200);
        assertFalse(detail.has("address"));
        assertFalse(detail.has("latitude"));
        assertFalse(detail.has("max_price"));
        assertEquals("CANCELADO", detail.path("assignments").get(0).path("status").asText());
        assertTrue(request(worker.token, "GET", "/api/jobs?mine=true", null, 200).path("total").asLong() > 0);
        request(client.token, "POST", "/api/jobs/" + job + "/cancel", Map.of("reason", "Ya no necesito el servicio."), 200);
        request(worker.token, "POST", "/api/reports", Map.of("jobId", job, "reason", "OTRO", "detail", "Quiero dejar constancia de la cancelacion."), 200);
    }

    @Test void avatarCannotBeReusedAsJobAttachment() throws Exception {
        Account client = account("CLIENTE");
        var image = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", image);
        var upload = mvc.perform(multipart("/api/media")
                .file(new MockMultipartFile("file", "avatar.png", "image/png", image.toByteArray()))
                .header("Authorization", "Bearer " + client.token)).andReturn().getResponse();
        assertEquals(201, upload.getStatus());
        String media = json.readTree(upload.getContentAsByteArray()).path("id").asText();
        var profile = new LinkedHashMap<String, Object>();
        profile.put("name", "Cliente de prueba"); profile.put("phone", "3000000000");
        profile.put("city", "Pasto"); profile.put("bio", ""); profile.put("experience", 0);
        profile.put("skills", List.of()); profile.put("radiusKm", 20); profile.put("available", true);
        profile.put("avatarId", media);
        request(client.token, "PUT", "/api/me", profile, 200);
        var job = publication(1); job.put("mediaIds", List.of(media));
        request(client.token, "POST", "/api/jobs", job, 403);
        profile.put("avatarId", "");
        assertTrue(request(client.token, "PUT", "/api/me", profile, 200).path("avatar_id").isNull());
        request(client.token, "POST", "/api/jobs", job, 201);
    }

    @Test void errorsPreserveHttpStatusAndRequestCorrelation() throws Exception {
        Account client = account("CLIENTE");
        request(client.token, "POST", "/api/me", Map.of(), 405);
        request(client.token, "GET", "/api/does-not-exist", null, 404);
        request(client.token, "GET", "/api/jobs?date=not-a-date", null, 400);
        request(client.token, "GET", "/api/jobs?page=2147483647", null, 200);
        var result = mvc.perform(get("/api/jobs/9223372036854775807")
                .header("Authorization", "Bearer " + client.token)).andReturn().getResponse();
        assertEquals(404, result.getStatus());
        JsonNode body = json.readTree(result.getContentAsByteArray());
        assertEquals(result.getHeader("X-Request-ID"), body.path("requestId").asText());
        assertFalse(body.has("trace"));
        request(null, "GET", "/api/me", null, 401);
    }

    @Test void adminMayModerateButOrdinaryUsersCannot() throws Exception {
        Account admin = account("CLIENTE"), worker = account("TRABAJADOR");
        db.update("UPDATE users SET role='ADMIN' WHERE id=:id", p("id", admin.id));
        request(admin.token, "GET", "/api/admin/summary", null, 200);
        request(admin.token, "GET", "/api/admin/audit", null, 200);
        request(worker.token, "GET", "/api/admin/audit", null, 403);
        request(admin.token, "PATCH", "/api/admin/users/" + worker.id,
                Map.of("active", false, "reason", "Suspension de prueba controlada."), 200);
        request(worker.token, "GET", "/api/me", null, 401);
        assertEquals(0, db.count("SELECT COUNT(*) FROM auth_sessions WHERE user_id=:id", p("id", worker.id)));
    }

    @Test void registrationAndPublicationValidationProtectData() throws Exception {
        Account client = account("CLIENTE");
        var invalid = publication(1); invalid.put("scheduledAt", OffsetDateTime.now().minusDays(1));
        request(client.token, "POST", "/api/jobs", invalid, 400);
        invalid = publication(1); invalid.put("maxPrice", 1000);
        request(client.token, "POST", "/api/jobs", invalid, 400);
        invalid = publication(1); invalid.put("latitude", 1.2);
        request(client.token, "POST", "/api/jobs", invalid, 400);
        var register = new LinkedHashMap<String, Object>();
        register.put("name", "Cuenta duplicada"); register.put("email", client.email);
        register.put("password", "Prueba2026!"); register.put("role", "CLIENTE");
        register.put("phone", "3000000000"); register.put("city", "Pasto");
        register.put("skills", List.of()); register.put("experience", 0);
        register.put("radiusKm", 20); register.put("acceptedTerms", true);
        request(null, "POST", "/api/auth/register", register, 409);
        register.put("email", UUID.randomUUID() + "@test.invalid"); register.put("role", "ADMIN");
        request(null, "POST", "/api/auth/register", register, 400);
    }
}
