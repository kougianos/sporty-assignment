package com.sporty.tracker.service;

import com.sporty.tracker.dto.ScoreEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScorePollingService {

    private final EventStateService eventStateService;
    private final ExternalApiClient externalApiClient;
    private final ScoreEventProducer scoreEventProducer;

    @Scheduled(fixedDelayString = "${app.polling.interval-ms}")
    public void pollLiveEventScores() {
        var liveEventIds = eventStateService.getLiveEventIds();
        if (liveEventIds.isEmpty()) {
            log.debug("No live events to poll");
            return;
        }

        log.info("Polling scores for {} live event(s)", liveEventIds.size());

        for (String eventId : liveEventIds) {
            pollScoreForEvent(eventId);
        }
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
