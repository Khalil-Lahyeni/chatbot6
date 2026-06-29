package com.actia.tracking_service.integration;

import com.actia.tracking_service.dto.TrainSystemStatusDto;
import com.actia.tracking_service.entity.Train;
import com.actia.tracking_service.enums.SystemHealthStatus;
import com.actia.tracking_service.repository.TrainRepository;
import com.actia.tracking_service.repository.TrainSystemStatusRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class TrainSystemStatusConsumerIT {

    // ── Containers ────────────────────────────────────────────────────────────

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"));

    /** Native Apache Kafka — org.testcontainers.kafka.KafkaContainer (Testcontainers 1.20+). */
    @Container
    static KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"));

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",          postgres::getJdbcUrl);
        registry.add("spring.datasource.username",     postgres::getUsername);
        registry.add("spring.datasource.password",     postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.redis.host",         redis::getHost);
        registry.add("spring.data.redis.port",         () -> redis.getMappedPort(6379));
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                     () -> "http://localhost/test-issuer");
    }

    // ── Test fixtures ─────────────────────────────────────────────────────────

    @Autowired private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired private TrainSystemStatusRepository systemStatusRepository;
    @Autowired private TrainRepository trainRepository;
    @Autowired private ObjectMapper objectMapper;

    private static final Long TRAIN_ID = 200L;

    @BeforeEach
    void setUp() {
        systemStatusRepository.deleteAll();
        if (trainRepository.findById(TRAIN_ID).isEmpty()) {
            Train t = new Train();
            t.setTrainId(TRAIN_ID);
            t.setName("IT-Train-Status");
            trainRepository.save(t);
        }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("New system-status message is persisted in PostgreSQL")
    void newStatusMessage_isPersisted() throws Exception {
        TrainSystemStatusDto dto = new TrainSystemStatusDto(
                TRAIN_ID, SystemHealthStatus.OK, SystemHealthStatus.OK, SystemHealthStatus.OK);

        kafkaTemplate.send("train-status", String.valueOf(TRAIN_ID), objectMapper.writeValueAsString(dto));

        await().atMost(Duration.ofSeconds(15))
               .untilAsserted(() ->
                       assertThat(systemStatusRepository.findAll()).hasSize(1));

        var saved = systemStatusRepository.findAll().get(0);
        assertThat(saved.getPacisStatus()).isEqualTo(SystemHealthStatus.OK);
        assertThat(saved.getCctvStatus()).isEqualTo(SystemHealthStatus.OK);
    }

    @Test
    @DisplayName("Duplicate system-status message is NOT persisted a second time")
    void duplicateStatusMessage_isNotPersisted() throws Exception {
        TrainSystemStatusDto dto = new TrainSystemStatusDto(
                TRAIN_ID, SystemHealthStatus.FAILURE, SystemHealthStatus.OK, SystemHealthStatus.PARTIALLY_OK);
        String json = objectMapper.writeValueAsString(dto);

        kafkaTemplate.send("train-status", String.valueOf(TRAIN_ID), json);
        await().atMost(Duration.ofSeconds(15))
               .untilAsserted(() -> assertThat(systemStatusRepository.findAll()).hasSize(1));

        kafkaTemplate.send("train-status", String.valueOf(TRAIN_ID), json);

        Thread.sleep(3_000);
        assertThat(systemStatusRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Changed system-status is persisted as a new record")
    void changedStatusMessage_isPersisted() throws Exception {
        TrainSystemStatusDto dto1 = new TrainSystemStatusDto(
                TRAIN_ID, SystemHealthStatus.OK, SystemHealthStatus.OK, SystemHealthStatus.OK);
        TrainSystemStatusDto dto2 = new TrainSystemStatusDto(
                TRAIN_ID, SystemHealthStatus.FAILURE, SystemHealthStatus.OK, SystemHealthStatus.OK);

        kafkaTemplate.send("train-status", String.valueOf(TRAIN_ID), objectMapper.writeValueAsString(dto1));
        await().atMost(Duration.ofSeconds(15))
               .untilAsserted(() -> assertThat(systemStatusRepository.findAll()).hasSize(1));

        kafkaTemplate.send("train-status", String.valueOf(TRAIN_ID), objectMapper.writeValueAsString(dto2));
        await().atMost(Duration.ofSeconds(15))
               .untilAsserted(() -> assertThat(systemStatusRepository.findAll()).hasSize(2));
    }
}
