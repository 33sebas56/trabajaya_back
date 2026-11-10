package com.trabajaya.storage;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
@Component @ConditionalOnProperty(name="app.storage.provider",havingValue="supabase")
public class SupabaseObjectStorage implements ObjectStorage {
    private final RestClient rest;
    private final String bucket;
    public SupabaseObjectStorage(@Value("${app.supabase.url}")String url,@Value("${app.supabase.key}")String key,@Value("${app.supabase.bucket}")String bucket){
        if(url.isBlank()||key.isBlank()||!bucket.matches("[a-z0-9-]+"))throw new IllegalArgumentException("Configura SUPABASE_URL, SUPABASE_SERVICE_KEY y un bucket privado valido.");
        if(!"https".equals(URI.create(url).getScheme()))throw new IllegalArgumentException("Supabase debe usar HTTPS.");
        this.bucket=bucket;
        var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(25));
        var builder=RestClient.builder().requestFactory(factory).baseUrl(url.replaceAll("/+$","")+"/storage/v1").defaultHeader("apikey",key);
        // Legacy service_role JWTs use Authorization too; modern sb_secret keys use apikey only.
        if(!key.startsWith("sb_secret_"))builder.defaultHeader("Authorization","Bearer "+key);
        rest=builder.build();
    }
    public String provider(){
        return "supabase";
    }
    public void write(String key,byte[] bytes,String type){
        rest.post().uri("/object/"+bucket+"/"+key).contentType(MediaType.parseMediaType(type)).header("x-upsert","false").body(bytes).retrieve().toBodilessEntity();
    }
    public byte[] read(String key){
        return rest.get().uri("/object/authenticated/"+bucket+"/"+key).retrieve().body(byte[].class);
    }
    public void delete(String key){
        rest.method(HttpMethod.DELETE).uri("/object/"+bucket).contentType(MediaType.APPLICATION_JSON).body(Map.of("prefixes",java.util.List.of(key))).retrieve().toBodilessEntity();
    }
}
