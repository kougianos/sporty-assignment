package com.sporty.tracker.service;

import com.sporty.tracker.dto.ScoreEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScorePollingService {

    private final EventStateService eventStateService;
    private final ExternalApiClient externalApiClient;
    private final ScoreEventProducer scoreEventProducer;

    private final ExecutorService pollingExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Scheduled(fixedDelayString = "${app.polling.interval-ms}")
    public void pollLiveEventScores() {
        var liveEventIds = eventStateService.getLiveEventIds();
        if (liveEventIds.isEmpty()) {
            log.debug("No live events to poll");
            return;
        }

        log.info("Polling scores for {} live event(s) in parallel", liveEventIds.size());

        var futures = liveEventIds.stream()
                .map(eventId -> CompletableFuture.runAsync(() -> pollScoreForEvent(eventId), pollingExecutor))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).join();
    }

    private void pollScoreForEvent(String eventId) {
        try {
            var scoreResponse = externalApiClient.fetchScore(eventId);
            var message = ScoreEventMessage.builder()
                    .eventId(scoreResponse.eventId())
                    .currentScore(scoreResponse.currentScore())
                    .timestamp(Instant.now())
                    .build();

            scoreEventProducer.publish(message);
        } catch (RestClientException e) {
            log.error("Failed to fetch score for eventId={} after retries: {}", eventId, e.getMessage());
        }
    }
}
