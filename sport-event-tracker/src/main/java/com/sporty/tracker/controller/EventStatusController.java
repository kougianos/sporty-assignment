package com.sporty.tracker.controller;

import com.sporty.tracker.dto.EventStatusUpdateRequest;
import com.sporty.tracker.dto.EventStatusUpdateResponse;
import com.sporty.tracker.service.EventStateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventStatusController {

    private final EventStateService eventStateService;

    @PostMapping("/status")
    public ResponseEntity<EventStatusUpdateResponse> updateEventStatus(
            @Valid @RequestBody EventStatusUpdateRequest request) {
        log.info("Received status update: eventId={}, status={}", request.eventId(), request.status());

        boolean changed = eventStateService.updateStatus(request.eventId(), request.status());
        var message = changed
                ? "Event %s status updated to %s".formatted(request.eventId(), request.status())
                : "Event %s was already %s".formatted(request.eventId(), request.status());

        return ResponseEntity.ok(new EventStatusUpdateResponse(
                request.eventId(),
                request.status().getValue(),
                message
        ));
    }
}
