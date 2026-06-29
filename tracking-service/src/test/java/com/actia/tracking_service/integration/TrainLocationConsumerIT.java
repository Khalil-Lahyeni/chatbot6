package com.actia.tracking_service.integration;

import com.actia.tracking_service.dto.TrainLocationDto;
import com.actia.tracking_service.entity.Train;
import com.actia.tracking_service.repository.TrainLocationRepository;
import com.actia.tracking_service.repository.TrainRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class TrainLocationConsumerIT {

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
        registry.add("spring.datasource.url",            postgres::getJdbcUrl);
        registry.add("spring.datasource.username",       postgres::getUsername);
        registry.add("spring.datasource.password",       postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers",   kafka::getBootstrapServers);
        registry.add("spring.data.redis.host",           redis::getHost);
        registry.add("spring.data.redis.port",           () -> redis.getMappedPort(6379));
        // Disable OAuth2 issuer validation in tests
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                     () -> "http://localhost/test-issuer");
    }

    // ── Test fixtures ─────────────────────────────────────────────────────────

    @Autowired private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired private TrainLocationRepository trainLocationRepository;
    @Autowired private TrainRepository trainRepository;
    @Autowired private ObjectMapper objectMapper;

    private static final Long TRAIN_ID = 100L;

    @BeforeEach
    void setUp() {
        trainLocationRepository.deleteAll();
        if (trainRepository.findById(TRAIN_ID).isEmpty()) {
            Train t = new Train();
            t.setTrainId(TRAIN_ID);
            t.setName("IT-Train");
            trainRepository.save(t);
        }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("New location message is persisted in PostgreSQL")
    void newMessage_isPersisted() throws Exception {
        TrainLocationDto dto = new TrainLocationDto(TRAIN_ID, "Lyon", "Paris", "Marseille");
        kafkaTemplate.send("train-location", String.valueOf(TRAIN_ID), objectMapper.writeValueAsString(dto));

        await().atMost(Duration.ofSeconds(15))
               .untilAsserted(() ->
                       assertThat(trainLocationRepository.findAll()).hasSize(1));
    }

    @Test
    @DisplayName("Duplicate location message is NOT persisted a second time")
    void duplicateMessage_isNotPersisted() throws Exception {
        TrainLocationDto dto = new TrainLocationDto(TRAIN_ID, "Lyon", "Paris", "Marseille");
        String json = objectMapper.writeValueAsString(dto);

        kafkaTemplate.send("train-location", String.valueOf(TRAIN_ID), json);
        await().atMost(Duration.ofSeconds(15))
               .untilAsserted(() -> assertThat(trainLocationRepository.findAll()).hasSize(1));

        kafkaTemplate.send("train-location", String.valueOf(TRAIN_ID), json);

        // Allow extra processing time then assert no second row was written
        Thread.sleep(3_000);
        assertThat(trainLocationRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Processed location event is published to train-events topic")
    void newMessage_publishesEventToTrainEvents() throws Exception {
        TrainLocationDto dto = new TrainLocationDto(TRAIN_ID, "Nice", "Cannes", "Monaco");

        try (KafkaConsumer<String, String> consumer = buildTestConsumer()) {
            consumer.subscribe(List.of("train-events"));

            kafkaTemplate.send("train-location", String.valueOf(TRAIN_ID), objectMapper.writeValueAsString(dto));

            List<ConsumerRecord<String, String>> received = new ArrayList<>();
            await().atMost(Duration.ofSeconds(20))
                   .untilAsserted(() -> {
                       consumer.poll(Duration.ofMillis(500)).forEach(received::add);
                       assertThat(received).isNotEmpty();
                   });

            assertThat(received.get(0).key()).isEqualTo(String.valueOf(TRAIN_ID));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private KafkaConsumer<String, String> buildTestConsumer() {
        return new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,        kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG,                 "it-event-verifier",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,        "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        ));
    }
}
