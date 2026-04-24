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
public class ScoreEventProducer {

    private final KafkaTemplate<String, ScoreEventMessage> kafkaTemplate;
    private final DlqProducer dlqProducer;

    @Value("${app.kafka.topic}")
    private String topic;

    public void publish(ScoreEventMessage message) {
        kafkaTemplate.send(topic, message.eventId(), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish score event for eventId={}: {}", message.eventId(), ex.getMessage());
                        dlqProducer.sendToDlq(message);
                    } else {
                        log.info("Published score event for eventId={} to topic={}, offset={}",
                                message.eventId(), topic, result.getRecordMetadata().offset());
                    }
                });
    }
}
