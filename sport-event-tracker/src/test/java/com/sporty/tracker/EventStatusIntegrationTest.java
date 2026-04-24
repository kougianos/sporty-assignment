package com.sporty.tracker;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.sporty.tracker.dto.ScoreEventMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Map;
import java.util.stream.StreamSupport;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class EventStatusIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:3.8.1");

    static WireMockServer wireMock = new WireMockServer(0);

    @Autowired
    TestRestTemplate restTemplate;

    @BeforeAll
    static void startWireMock() {
        wireMock.start();
        WireMock.configureFor("localhost", wireMock.port());
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        TestKafkaHelper.configureKafkaProperties(registry, kafka);
        registry.add("app.external-api.base-url", () -> "http://localhost:" + wireMock.port());
        registry.add("app.polling.interval-ms", () -> "100");
        registry.add("app.external-api.retry.delay-ms", () -> "100");
        registry.add("app.external-api.retry.multiplier", () -> "2");
        registry.add("app.kafka.publish.retry.delay-ms", () -> "100");
        registry.add("app.kafka.publish.retry.multiplier", () -> "1");
    }

    @Test
    void shouldAcceptLiveStatusUpdate() {
        var body = Map.of("eventId", "event-1", "status", "live");

        var response = restTemplate.postForEntity("/events/status", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("eventId", "event-1");
        assertThat(response.getBody()).containsEntry("status", "live");
    }

    @Test
    void shouldAcceptNotLiveStatusUpdate() {
        var bodyLive = Map.of("eventId", "event-2", "status", "live");
        restTemplate.postForEntity("/events/status", bodyLive, Map.class);

        var bodyNotLive = Map.of("eventId", "event-2", "status", "not live");
        var response = restTemplate.postForEntity("/events/status", bodyNotLive, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "not live");
    }

    @Test
    void shouldReturnBadRequestForInvalidPayload() {
        var body = Map.of("eventId", "", "status", "live");

        var response = restTemplate.postForEntity("/events/status", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturnBadRequestForInvalidStatus() {
        var body = Map.of("eventId", "event-3", "status", "INVALID");

        var response = restTemplate.postForEntity("/events/status", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldAcceptNumericEventId() {
        var body = Map.<String, Object>of("eventId", 1234, "status", "live");

        var response = restTemplate.postForEntity("/events/status", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("eventId", "1234");
        assertThat(response.getBody()).containsEntry("status", "live");
    }

    @Test
    void shouldAcceptBooleanStatusTrue() {
        var body = Map.<String, Object>of("eventId", "event-bool-1", "status", true);

        var response = restTemplate.postForEntity("/events/status", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "live");
    }

    @Test
    void shouldAcceptBooleanStatusFalse() {
        // First set live so NOT_LIVE is meaningful
        restTemplate.postForEntity("/events/status",
                Map.<String, Object>of("eventId", "event-bool-2", "status", true), Map.class);

        var body = Map.<String, Object>of("eventId", "event-bool-2", "status", false);
        var response = restTemplate.postForEntity("/events/status", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "not live");
    }

    @Test
    void shouldAcceptNumericEventIdWithBooleanStatus() {
        var body = Map.<String, Object>of("eventId", 9999, "status", true);

        var response = restTemplate.postForEntity("/events/status", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("eventId", "9999");
        assertThat(response.getBody()).containsEntry("status", "live");
    }

    @Test
    void shouldPollExternalApiAndPublishToKafka() {
        wireMock.stubFor(get(urlPathMatching("/api/scores/.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"eventId": "event-poll", "currentScore": "2:1"}
                                """)));

        var body = Map.of("eventId", "event-poll", "status", "live");
        restTemplate.postForEntity("/events/status", body, Map.class);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            try (var consumer = TestKafkaHelper.<ScoreEventMessage>createConsumer(kafka, "sport-event-scores")) {
                var records = TestKafkaHelper.pollUntilRecords(consumer, Duration.ofSeconds(2));

                var matchingRecord = StreamSupport.stream(records.spliterator(), false)
                        .filter(r -> "event-poll".equals(r.key()))
                        .findFirst();

                assertThat(matchingRecord).isPresent();
                assertThat(matchingRecord.get().value().eventId()).isEqualTo("event-poll");
                assertThat(matchingRecord.get().value().currentScore()).isEqualTo("2:1");
            }
        });
    }

    @Test
    void shouldStopPollingWhenEventSetToNotLive() {
        wireMock.stubFor(get(urlPathMatching("/api/scores/.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"eventId": "event-stop", "currentScore": "1:0"}
                                """)));

        restTemplate.postForEntity("/events/status", Map.of("eventId", "event-stop", "status", "live"), Map.class);

        // Wait until at least one poll has been made for this event
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            var reqs = wireMock.findAll(getRequestedFor(urlPathMatching("/api/scores/event-stop")));
            assertThat(reqs).isNotEmpty();
        });

        restTemplate.postForEntity("/events/status", Map.of("eventId", "event-stop", "status", "not live"), Map.class);
        wireMock.resetRequests();

        // Wait a couple of polling cycles and verify no new requests are made
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            var requests = wireMock.findAll(getRequestedFor(urlPathMatching("/api/scores/event-stop")));
            assertThat(requests).isEmpty();
        });
    }

    @Test
    void shouldHandleExternalApiFailureGracefully() {
        wireMock.stubFor(get(urlPathMatching("/api/scores/.*"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        var body = Map.of("eventId", "event-fail", "status", "live");
        var response = restTemplate.postForEntity("/events/status", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Wait for polling + retries (3 attempts: initial + 2 retries)
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var requests = wireMock.findAll(getRequestedFor(urlPathMatching("/api/scores/event-fail")));
            assertThat(requests).hasSizeGreaterThanOrEqualTo(3);
        });
    }
}
