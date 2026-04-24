package com.sporty.tracker.service;

import com.sporty.tracker.dto.ExternalScoreResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalApiClient {

    private final RestTemplate restTemplate;

    @Value("${app.external-api.base-url}")
    private String baseUrl;

    @Value("${app.external-api.scores-path}")
    private String scoresPath;

    @Retryable(
            retryFor = RestClientException.class,
            maxAttempts = 3,
            backoff = @Backoff(delayExpression = "${app.external-api.retry.delay-ms:1000}",
                    multiplierExpression = "${app.external-api.retry.multiplier:2}")
    )
    public ExternalScoreResponse fetchScore(String eventId) {
        var url = baseUrl + scoresPath.replace("{eventId}", eventId);
        log.debug("Fetching score for event {} from {}", eventId, url);
        return restTemplate.getForObject(url, ExternalScoreResponse.class);
    }
}
