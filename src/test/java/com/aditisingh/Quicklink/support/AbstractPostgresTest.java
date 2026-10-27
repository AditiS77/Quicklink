package com.aditisingh.Quicklink.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Starts ONE throw-away PostgreSQL container for the whole test run and points the
 * Spring datasource at it. Flyway then runs your real migrations against a real
 * Postgres, so schema/entity drift is caught by the tests.
 *
 * Requires Docker to be running on the machine executing the tests.
 */
public abstract class AbstractPostgresTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
