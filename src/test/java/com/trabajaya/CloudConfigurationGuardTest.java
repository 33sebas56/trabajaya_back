package com.trabajaya;

import com.trabajaya.config.CloudConfigurationGuard;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class CloudConfigurationGuardTest {

    @TempDir
    Path directory;

    private MockEnvironment environment() throws IOException {
        Path certificate = directory.resolve("aiven-ca.pem");

        // Solo comprueba presencia y lectura.
        // El driver valida el certificado real al establecer la conexion.
        Files.writeString(certificate, "test fixture; not a real certificate");

        String path = URLEncoder.encode(
            certificate.toString(), StandardCharsets.UTF_8);

        var env = new MockEnvironment();
        env.setActiveProfiles("postgres", "render");

        return env.withProperty("spring.datasource.url",
                "jdbc:postgresql://database.test:5432/app"
                    + "?sslmode=verify-full&sslrootcert=" + path)
            .withProperty("spring.datasource.username", "test_user")
            .withProperty("spring.datasource.password", "ci-only-password")
            .withProperty("app.storage.provider", "supabase");
    }

    private void validate(MockEnvironment env) {
        var guard = new CloudConfigurationGuard();
        guard.setEnvironment(env);
        guard.postProcessBeanFactory(new DefaultListableBeanFactory());
    }

    @Test
    void acceptsCloudConfigurationWithoutBootstrapAdmin() throws IOException {
        assertDoesNotThrow(() -> validate(environment()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "postgres://host/db",
        "postgresql://host/db",
        "jdbc:mysql://host/db",
        ""
    })
    void rejectsNonJdbcPostgresUrls(String url) throws IOException {
        var env = environment().withProperty("spring.datasource.url", url);

        var error = assertThrows(
            IllegalStateException.class, () -> validate(env));

        assertTrue(error.getMessage().contains("jdbc:postgresql://"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"require", "disable", "verify-ca"})
    void rejectsUnverifiedTls(String mode) throws IOException {
        var env = environment();

        String url = env.getProperty("spring.datasource.url")
            .replace("sslmode=verify-full", "sslmode=" + mode);

        env.withProperty("spring.datasource.url", url);

        assertThrows(IllegalStateException.class, () -> validate(env));
    }

    @Test
    void rejectsRepeatedSslMode() throws IOException {
        var env = environment();

        env.withProperty("spring.datasource.url",
            env.getProperty("spring.datasource.url") + "&sslmode=require");

        assertThrows(IllegalStateException.class, () -> validate(env));
    }

    @Test
    void rejectsCredentialsInUrlWithoutEchoingThem() throws IOException {
        for (String url : new String[] {
            "jdbc:postgresql://user:private-test-value@host/db",
            "jdbc:postgresql://host/db?password=private-test-value",
            "jdbc:postgresql://host/db?password=private-test-value%ZZ"
        }) {
            var env = environment().withProperty("spring.datasource.url", url);

            var error = assertThrows(
                IllegalStateException.class, () -> validate(env));

            var trace = new StringWriter();
            error.printStackTrace(new PrintWriter(trace));

            assertFalse(trace.toString().contains("private-test-value"));
        }
    }

    @Test
    void rejectsMissingCertificate() throws IOException {
        var env = environment();
        Files.delete(directory.resolve("aiven-ca.pem"));

        var error = assertThrows(
            IllegalStateException.class, () -> validate(env));

        assertTrue(error.getMessage().contains("Secret Files"));
    }

    @Test
    void rejectsMissingPostgresProfile() throws IOException {
        var env = environment();
        env.setActiveProfiles("render");

        assertThrows(IllegalStateException.class, () -> validate(env));
    }

    @ParameterizedTest
    @CsvSource({
        "app.seed.enabled,true",
        "app.storage.provider,local",
        "app.reset.console,true"
    })
    void rejectsUnsafeDefaults(String key, String value) throws IOException {
        var env = environment().withProperty(key, value);

        assertThrows(IllegalStateException.class, () -> validate(env));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "spring.datasource.username",
        "spring.datasource.password"
    })
    void rejectsMissingDatabaseCredentials(String key) throws IOException {
        var env = environment().withProperty(key, "");

        assertThrows(IllegalStateException.class, () -> validate(env));
    }

    @Test
    void validatesOptionalAdministrator() throws IOException {
        var env = environment()
            .withProperty("app.admin.email", "admin@example.invalid");

        assertThrows(IllegalStateException.class, () -> validate(env));

        env.withProperty("app.admin.password", "123");

        assertThrows(IllegalStateException.class, () -> validate(env));

        env.withProperty("app.admin.password", "Administrador2026!");

        assertDoesNotThrow(() -> validate(env));
    }

    @Test
    void rejectsBadUrlBeforeCreatingApplicationBeans() throws IOException {
        var env = environment()
            .withProperty("spring.datasource.url", "postgres://host/db");

        var created = new AtomicBoolean(false);

        try (var context = new AnnotationConfigApplicationContext()) {
            context.setEnvironment(env);
            context.register(CloudConfigurationGuard.class);

            context.registerBean("databaseProbe", Object.class, () -> {
                created.set(true);
                return new Object();
            });

            var error = assertThrows(
                IllegalStateException.class, context::refresh);

            assertTrue(error.getMessage().contains("DB_URL"));
            assertFalse(
                created.get(),
                "No debe iniciar beans antes de validar la configuracion."
            );
        }
    }
}
