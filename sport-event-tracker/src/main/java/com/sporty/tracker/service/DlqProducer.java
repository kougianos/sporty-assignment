package com.sporty.tracker.service;

import com.sporty.tracker.dto.ScoreEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DlqProducer {

    private final KafkaTemplate<String, ScoreEventMessage> kafkaTemplate;

    @Value("${app.kafka.dlq-topic}")
    private String dlqTopic;

    public void sendToDlq(ScoreEventMessage message) {
        kafkaTemplate.send(dlqTopic, message.eventId(), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("CRITICAL: Failed to publish to DLQ for eventId={}: {}", message.eventId(), ex.getMessage());
                    } else {
                        log.warn("Sent failed message to DLQ for eventId={}, offset={}",
                                message.eventId(), result.getRecordMetadata().offset());
                    }
                });
    }
}
