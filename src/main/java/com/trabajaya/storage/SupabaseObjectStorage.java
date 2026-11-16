package com.trabajaya.storage;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "supabase")
public class SupabaseObjectStorage implements ObjectStorage {
    private final RestClient rest;
    private final String bucket;

    public SupabaseObjectStorage(
            @Value("${app.supabase.url}") String url,
            @Value("${app.supabase.key}") String key,
            @Value("${app.supabase.bucket}") String bucket) {
        if (url.isBlank() || key.isBlank() || !bucket.matches("[a-z0-9][a-z0-9-]{0,62}")) {
            throw new IllegalArgumentException("Configura SUPABASE_URL, SUPABASE_SERVICE_KEY y SUPABASE_BUCKET.");
        }
        URI uri = URI.create(url);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || !(uri.getPath().isEmpty() || "/".equals(uri.getPath()))) {
            throw new IllegalArgumentException("SUPABASE_URL debe ser la URL HTTPS del proyecto, sin rutas ni credenciales.");
        }
        if (key.startsWith("sb_publishable_") || !(key.startsWith("sb_secret_") || key.startsWith("eyJ"))) {
            throw new IllegalArgumentException("Usa una clave secreta de servidor o la service_role; no una clave publica.");
        }
        this.bucket = bucket;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(25));
        var builder = RestClient.builder().requestFactory(factory)
                .baseUrl(url.replaceAll("/+$", "") + "/storage/v1")
                .defaultHeader("apikey", key);
        // Modern secrets are not JWTs; they go only in apikey.
        if (!key.startsWith("sb_secret_")) builder.defaultHeader("Authorization", "Bearer " + key);
        this.rest = builder.build();
    }

    @Override
    public String provider() { return "supabase"; }

    @Override
    public void write(String key, byte[] bytes, String type) {
        ObjectKey.require(key);
        try {
            rest.post().uri("/object/" + bucket + "/" + key)
                    .contentType(MediaType.parseMediaType(type)).header("x-upsert", "false")
                    .body(bytes).retrieve().toBodilessEntity();
        } catch (RestClientException error) {
            throw failure(error, false);
        }
    }

    @Override
    public byte[] read(String key) {
        ObjectKey.require(key);
        try {
            byte[] result = rest.get().uri("/object/authenticated/" + bucket + "/" + key)
                    .retrieve().body(byte[].class);
            if (result == null) throw new StorageException(StorageException.Reason.MISSING);
            return result;
        } catch (RestClientException error) {
            throw failure(error, true);
        }
    }

    @Override
    public void delete(String key) {
        ObjectKey.require(key);
        try {
            rest.method(HttpMethod.DELETE).uri("/object/" + bucket)
                    .contentType(MediaType.APPLICATION_JSON).body(Map.of("prefixes", List.of(key)))
                    .retrieve().toBodilessEntity();
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() != 404) throw failure(error, false);
        } catch (RestClientException error) {
            throw failure(error, false);
        }
    }

    private StorageException failure(RestClientException error, boolean reading) {
        if (error instanceof RestClientResponseException response) {
            int status = response.getStatusCode().value();
            if (reading && status == 404) return new StorageException(StorageException.Reason.MISSING);
            if (status >= 400 && status < 500 && status != 429) {
                return new StorageException(StorageException.Reason.REJECTED);
            }
        }
        return new StorageException(StorageException.Reason.UNAVAILABLE);
    }
}
