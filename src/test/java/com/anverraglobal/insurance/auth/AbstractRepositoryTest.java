package com.anverraglobal.insurance.auth;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for repository integration tests using a real PostgreSQL Testcontainer.
 *
 * <p>Liquibase migrations are applied automatically by Spring Boot's auto-configuration
 * when the context starts. Each test method is wrapped in a transaction that is rolled
 * back, keeping the database clean between tests.</p>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({LiquibaseAutoConfiguration.class, AbstractRepositoryTest.JpaAuditingTestConfig.class})
@Testcontainers
@ActiveProfiles("test")
@Transactional
@Rollback
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractRepositoryTest {

    @Autowired
    protected EntityManager entityManager;

    protected void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @TestConfiguration
    @EnableJpaAuditing
    static class JpaAuditingTestConfig {
    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("anverraglobal_test")
                    .withUsername("test")
                    .withPassword("test");
}
