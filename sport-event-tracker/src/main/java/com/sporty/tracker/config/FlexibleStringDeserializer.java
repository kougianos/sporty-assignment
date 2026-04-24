package com.sporty.tracker.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;

public class FlexibleStringDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return switch (p.currentToken()) {
            case VALUE_STRING -> p.getValueAsString();
            case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> p.getValueAsString();
            default -> throw new InvalidFormatException(
                    p, "Expected string or number for eventId", p.getText(), String.class);
        };
    }
}
