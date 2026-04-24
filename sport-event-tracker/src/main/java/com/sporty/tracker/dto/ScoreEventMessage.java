package com.sporty.tracker.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ScoreEventMessage(
        String eventId,
        String currentScore,
        Instant timestamp
) {
}
