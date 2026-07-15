package com.anverraglobal.insurance.auth;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for repository integration tests using a real PostgreSQL Testcontainer.
 *
 * <p>Note: Spring Boot 4.x removed {@code @DataJpaTest} and other slice annotations.
 * All JPA repository tests use the full {@code @SpringBootTest} context with
 * Testcontainers for a real PostgreSQL database.</p>
 *
 * <p>Liquibase migrations are applied automatically by Spring Boot's auto-configuration
 * when the context starts. Each test method is wrapped in a transaction that is rolled
 * back, keeping the database clean between tests.</p>
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
@Rollback
public abstract class AbstractRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("anverraglobal_test")
                    .withUsername("test")
                    .withPassword("test");
}
