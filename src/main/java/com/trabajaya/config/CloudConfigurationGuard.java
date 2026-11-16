package com.trabajaya.config;

import com.trabajaya.users.PasswordPolicy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Valida Render antes de crear el DataSource, Flyway o los servicios. */
@Component
@Profile("render")
public final class CloudConfigurationGuard
    implements BeanFactoryPostProcessor, EnvironmentAware, PriorityOrdered {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        require(Arrays.asList(environment.getActiveProfiles()).contains("postgres"),
            "Activa SPRING_PROFILES_ACTIVE=postgres,render.");

        validateDatabaseUrl(value("spring.datasource.url"));

        require(!value("spring.datasource.username").isBlank(),
            "Configura DB_USERNAME.");
        require(!value("spring.datasource.password").isBlank(),
            "Configura DB_PASSWORD.");
        require("supabase".equals(value("app.storage.provider")),
            "Configura STORAGE_PROVIDER=supabase; el disco de Render no es persistente.");
        require(!environment.getProperty("app.seed.enabled", Boolean.class, false),
            "No actives DEMO_SEED en el despliegue.");
        require(!environment.getProperty("app.reset.console", Boolean.class, false),
            "Los enlaces de recuperacion no deben escribirse en los logs.");

        String email = value("app.admin.email");
        String password = value("app.admin.password");

        require(email.isBlank() == password.isBlank(),
            "Configura ADMIN_EMAIL y ADMIN_PASSWORD juntos.");

        if (!email.isBlank()) {
            require(email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"),
                "ADMIN_EMAIL debe ser un correo valido.");
            require(PasswordPolicy.validAdministrator(password),
                "ADMIN_PASSWORD necesita 12 caracteres, letras y numeros, hasta 72 bytes.");
        }
    }

    private void validateDatabaseUrl(String url) {
        require(url.startsWith("jdbc:postgresql://"),
            "DB_URL debe comenzar con jdbc:postgresql://, no con postgres://.");

        URI uri;
        try {
            uri = new URI(url.substring("jdbc:".length()));
        } catch (URISyntaxException exception) {
            // No encadenar la excepcion: podria incluir una URL con credenciales.
            throw new IllegalStateException("DB_URL no tiene un formato JDBC valido.");
        }

        require(uri.getRawUserInfo() == null,
            "No incluyas credenciales en DB_URL; usa DB_USERNAME y DB_PASSWORD.");
        require(uri.getHost() != null && uri.getPath() != null
                && uri.getPath().length() > 1 && uri.getFragment() == null,
            "DB_URL debe indicar un host y una base de datos validos.");

        Map<String, String> parameters = queryParameters(uri.getRawQuery());

        require("verify-full".equals(parameters.get("sslmode")),
            "DB_URL requiere sslmode=verify-full.");

        String certificate = parameters.get("sslrootcert");
        require(certificate != null && !certificate.isBlank(),
            "Agrega sslrootcert=/etc/secrets/aiven-ca.pem a DB_URL.");

        boolean readable;
        try {
            Path path = Path.of(certificate);
            readable = path.isAbsolute()
                && Files.isRegularFile(path)
                && Files.isReadable(path);
        } catch (RuntimeException exception) {
            readable = false;
        }

        require(readable,
            "No se puede leer el CA de Aiven. Agrega aiven-ca.pem en Secret Files de Render.");
    }

    private Map<String, String> queryParameters(String query) {
        Map<String, String> parameters = new HashMap<>();

        if (query == null || query.isBlank()) {
            return parameters;
        }

        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            String key;
            String value;

            try {
                key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                value = pair.length == 2
                    ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException(
                    "DB_URL contiene parametros mal codificados.");
            }

            require(!key.equalsIgnoreCase("password")
                    && !key.equalsIgnoreCase("user")
                    && !key.equalsIgnoreCase("username"),
                "No incluyas credenciales en DB_URL; usa DB_USERNAME y DB_PASSWORD.");

            require(parameters.putIfAbsent(key, value) == null,
                "DB_URL contiene parametros repetidos.");
        }

        return parameters;
    }

    private String value(String key) {
        return environment.getProperty(key, "");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
