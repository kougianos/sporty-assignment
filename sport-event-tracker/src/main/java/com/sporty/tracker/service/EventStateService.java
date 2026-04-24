package com.sporty.tracker.service;

import com.sporty.tracker.model.EventStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class EventStateService {

    private final Set<String> liveEvents = ConcurrentHashMap.newKeySet();

    public boolean updateStatus(String eventId, EventStatus status) {
        return switch (status) {
            case LIVE -> {
                boolean added = liveEvents.add(eventId);
                if (added) {
                    log.info("Event {} is now LIVE", eventId);
                }
                yield added;
            }
            case NOT_LIVE -> {
                boolean removed = liveEvents.remove(eventId);
                if (removed) {
                    log.info("Event {} is now NOT_LIVE", eventId);
                }
                yield removed;
            }
        };
    }

    public Set<String> getLiveEventIds() {
        return Collections.unmodifiableSet(liveEvents);
    }

    public boolean isLive(String eventId) {
        return liveEvents.contains(eventId);
    }
}
