package com.modernization.customer.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/** Shared Testcontainers: PostgreSQL, Kafka and Redis, started once per JVM. */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("mirror.gcr.io/library/postgres:16-alpine").asCompatibleSubstituteFor("postgres"));

    @ServiceConnection
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.1"));

    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("mirror.gcr.io/library/redis:7-alpine")).withExposedPorts(6379);

    static {
        POSTGRES.start();
        KAFKA.start();
        REDIS.start();
    }
}
