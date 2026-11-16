package com.trabajaya;

import com.trabajaya.config.CloudConfigurationGuard;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class CloudConfigurationGuardTest {
    private MockEnvironment environment() {
        var env = new MockEnvironment();
        env.setActiveProfiles("postgres", "render");
        env.withProperty("spring.datasource.url", "jdbc:postgresql://database.test:5432/app?sslmode=verify-full")
                .withProperty("spring.datasource.password", "test-only-not-a-secret")
                .withProperty("app.storage.provider", "supabase");
        return env;
    }

    @Test void acceptsCloudConfigurationWithoutAnOptionalBootstrapAdmin() {
        assertDoesNotThrow(() -> new CloudConfigurationGuard(environment()).afterPropertiesSet());
    }

    @Test void rejectsDemoLocalStorageAndUnverifiedTls() {
        for (String[] setting : new String[][] {
                {"app.seed.enabled", "true"}, {"app.storage.provider", "local"},
                {"app.reset.console", "true"}, {"spring.datasource.url", "jdbc:postgresql://host/db?sslmode=require"}}) {
            var env = environment().withProperty(setting[0], setting[1]);
            assertThrows(IllegalStateException.class, () -> new CloudConfigurationGuard(env).afterPropertiesSet());
        }
    }

    @Test void rejectsIncompleteOrWeakAdministrator() {
        var env = environment().withProperty("app.admin.email", "admin@example.invalid");
        assertThrows(IllegalStateException.class, () -> new CloudConfigurationGuard(env).afterPropertiesSet());
        env.withProperty("app.admin.password", "123");
        assertThrows(IllegalStateException.class, () -> new CloudConfigurationGuard(env).afterPropertiesSet());
        env.withProperty("app.admin.password", "Administrador2026!");
        assertDoesNotThrow(() -> new CloudConfigurationGuard(env).afterPropertiesSet());
    }
}
