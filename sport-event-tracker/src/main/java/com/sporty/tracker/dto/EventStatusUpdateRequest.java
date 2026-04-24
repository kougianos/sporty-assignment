package com.sporty.tracker.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sporty.tracker.config.EventStatusDeserializer;
import com.sporty.tracker.config.FlexibleStringDeserializer;
import com.sporty.tracker.model.EventStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EventStatusUpdateRequest(
        @NotBlank(message = "eventId must not be blank")
        @JsonDeserialize(using = FlexibleStringDeserializer.class)
        String eventId,

        @NotNull(message = "status must not be null")
        @JsonDeserialize(using = EventStatusDeserializer.class)
        EventStatus status
) {
}
