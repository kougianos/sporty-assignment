package com.sporty.tracker.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.sporty.tracker.model.EventStatus;

import java.io.IOException;

public class EventStatusDeserializer extends JsonDeserializer<EventStatus> {

    @Override
    public EventStatus deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return switch (p.currentToken()) {
            case VALUE_TRUE -> EventStatus.LIVE;
            case VALUE_FALSE -> EventStatus.NOT_LIVE;
            case VALUE_STRING -> parseFromString(p, p.getValueAsString());
            default -> throw new InvalidFormatException(
                    p, "Expected boolean or string for status", p.getText(), EventStatus.class);
        };
    }

    private EventStatus parseFromString(JsonParser p, String value) throws IOException {
        return switch (value.trim().toLowerCase().replace("_", " ")) {
            case "live", "true" -> EventStatus.LIVE;
            case "not live", "not_live", "false" -> EventStatus.NOT_LIVE;
            default -> throw new InvalidFormatException(
                    p, "Invalid status value: '%s'. Expected: live, not live, true, false".formatted(value),
                    value, EventStatus.class);
        };
    }
}
