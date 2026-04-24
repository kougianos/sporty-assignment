package com.sporty.external.scheduler;

import com.sporty.external.dto.EventStatusPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventStatusSimulator {

    private static final List<Object> EVENT_IDS = List.of("1001", "1002", 1003, 1004, "1005");
    private static final List<Object> VALID_STATUSES = List.of("live", "not live", true, false);
    private static final List<Object> INVALID_STATUSES = List.of("INVALID", "PAUSED", "UNKNOWN", "");
    private static final List<Object> INVALID_EVENT_IDS = List.of("", " ");

    private final RestTemplate restTemplate;

    @Value("${app.tracker.base-url}")
    private String trackerBaseUrl;

    @Scheduled(fixedRate = 2000)
    public void simulateEventStatusUpdates() {
        var random = ThreadLocalRandom.current();
        boolean sendInvalid = random.nextInt(100) < 20;

        var payload = sendInvalid ? generateInvalidPayload(random) : generateValidPayload(random);
        var url = trackerBaseUrl + "/events/status";

        log.info("Sending {} payload to {}: {}", sendInvalid ? "INVALID" : "VALID", url, payload);

        try {
            var response = restTemplate.postForEntity(url, payload, String.class);
            log.info("Response: {} - {}", response.getStatusCode(), response.getBody());
        } catch (RestClientException e) {
            log.warn("Failed to call tracker: {}", e.getMessage());
        }
    }

    private EventStatusPayload generateValidPayload(ThreadLocalRandom random) {
        return new EventStatusPayload(
                randomFrom(EVENT_IDS, random),
                randomFrom(VALID_STATUSES, random)
        );
    }

    private EventStatusPayload generateInvalidPayload(ThreadLocalRandom random) {
        int scenario = random.nextInt(3);
        return switch (scenario) {
            case 0 -> new EventStatusPayload(
                    randomFrom(INVALID_EVENT_IDS, random),
                    randomFrom(VALID_STATUSES, random)
            );
            case 1 -> new EventStatusPayload(
                    randomFrom(EVENT_IDS, random),
                    randomFrom(INVALID_STATUSES, random)
            );
            default -> new EventStatusPayload(
                    randomFrom(INVALID_EVENT_IDS, random),
                    randomFrom(INVALID_STATUSES, random)
            );
        };
    }

    private static <T> T randomFrom(List<T> list, ThreadLocalRandom random) {
        return list.get(random.nextInt(list.size()));
    }
}
