package com.trabajaya.config;

import com.trabajaya.users.PasswordPolicy;
import java.util.Arrays;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Reject dangerous defaults before serving a public Render deployment. */
@Component
@Profile("render")
public class CloudConfigurationGuard implements InitializingBean {
    private final Environment environment;

    public CloudConfigurationGuard(Environment environment) { this.environment = environment; }

    @Override
    public void afterPropertiesSet() {
        require(Arrays.asList(environment.getActiveProfiles()).contains("postgres"),
                "Activa SPRING_PROFILES_ACTIVE=postgres,render.");
        String database = environment.getProperty("spring.datasource.url", "");
        require(database.startsWith("jdbc:postgresql://"), "DB_URL debe usar JDBC PostgreSQL.");
        require(database.contains("sslmode=verify-full"), "DB_URL debe verificar TLS con sslmode=verify-full y el CA de Aiven.");
        require(!environment.getProperty("spring.datasource.password", "").isBlank(), "Configura DB_PASSWORD.");
        require("supabase".equals(environment.getProperty("app.storage.provider")),
                "Configura STORAGE_PROVIDER=supabase; el disco de Render no es persistente.");
        require(!environment.getProperty("app.seed.enabled", Boolean.class, false),
                "No actives DEMO_SEED en un servicio publico.");
        require(!environment.getProperty("app.reset.console", Boolean.class, false),
                "Los enlaces de recuperacion no deben escribirse en logs publicos.");
        String email = environment.getProperty("app.admin.email", "");
        String password = environment.getProperty("app.admin.password", "");
        require(email.isBlank() == password.isBlank(), "Configura ADMIN_EMAIL y ADMIN_PASSWORD juntos.");
        if (!email.isBlank()) {
            require(email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"), "ADMIN_EMAIL debe ser un correo valido.");
            require(PasswordPolicy.validAdministrator(password),
                    "ADMIN_PASSWORD necesita 12 caracteres, letras y numeros, hasta 72 bytes.");
        }
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
