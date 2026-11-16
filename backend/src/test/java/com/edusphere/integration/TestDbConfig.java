package com.edusphere.integration;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Rebuilds the schema from the Flyway migrations for every test run. Refuses to touch a non-test database. */
@TestConfiguration
public class TestDbConfig {
    @Bean
    FlywayMigrationStrategy cleanMigrate() {
        return flyway -> {
            String catalog;
            try (var c = flyway.getConfiguration().getDataSource().getConnection()) {
                catalog = c.getCatalog();
            } catch (java.sql.SQLException e) {
                throw new IllegalStateException(e);
            }
            if (!"Edu-Sphere-test".equals(catalog)) {
                throw new IllegalStateException("Refusing to clean a database that is not the test database: " + catalog);
            }
            flyway.clean();
            flyway.migrate();
        };
    }
}
