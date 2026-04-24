package com.sporty.tracker.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventStatus {
    LIVE("live"),
    NOT_LIVE("not live");

    @JsonValue
    private final String value;
}
