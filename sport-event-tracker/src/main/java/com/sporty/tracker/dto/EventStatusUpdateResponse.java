package com.sporty.tracker.dto;

public record EventStatusUpdateResponse(
        String eventId,
        String status,
        String message
) {
}
