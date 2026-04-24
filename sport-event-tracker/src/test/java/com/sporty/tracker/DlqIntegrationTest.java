package com.sporty.tracker;

import com.sporty.tracker.dto.ScoreEventMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.polling.interval-ms=999999")
@Testcontainers
class DlqIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:3.8.1");

    @Value("${app.kafka.topic}")
    private String mainTopic;

    @Value("${app.kafka.dlq-topic}")
    private String dlqTopic;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        TestKafkaHelper.configureKafkaProperties(registry, kafka);
        registry.add("app.external-api.base-url", () -> "http://localhost:19999");
        registry.add("app.kafka.publish.retry.delay-ms", () -> "100");
        registry.add("app.kafka.publish.retry.multiplier", () -> "1");
    }

    @Test
    void shouldConsumeFromDlqAndRepublishToMainTopic() {
        var message = ScoreEventMessage.builder()
                .eventId("dlq-test-event")
                .currentScore("3:2")
                .timestamp(Instant.now())
                .build();

        // Step 1: Simulate a failed publish by writing directly to the DLQ topic
        TestKafkaHelper.produce(kafka, dlqTopic, message.eventId(), message);

        // Step 2: The DLQ consumer should pick it up and republish to the main topic.
        //         Verify the message eventually appears on the main topic.
        try (var consumer = TestKafkaHelper.<ScoreEventMessage>createConsumer(
                kafka, mainTopic)) {
            var records = TestKafkaHelper.pollUntilRecords(consumer, Duration.ofSeconds(15));

            var match = StreamSupport.stream(records.spliterator(), false)
                    .filter(r -> "dlq-test-event".equals(r.key()))
                    .findFirst();

            assertThat(match).isPresent();
            assertThat(match.get().value().eventId()).isEqualTo("dlq-test-event");
            assertThat(match.get().value().currentScore()).isEqualTo("3:2");
        }
    }
}
