package com.sporty.tracker.service;

import com.sporty.tracker.dto.ScoreEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreEventProducer {

    private final KafkaTemplate<String, ScoreEventMessage> kafkaTemplate;
    private final DlqProducer dlqProducer;

    @Value("${app.kafka.topic}")
    private String topic;

    @Retryable(
            retryFor = Exception.class,
            maxAttemptsExpression = "${app.kafka.publish.retry.max-attempts:3}",
            backoff = @Backoff(
                    delayExpression = "${app.kafka.publish.retry.delay-ms:500}",
                    multiplierExpression = "${app.kafka.publish.retry.multiplier:2}"
            )
    )
    public void publish(ScoreEventMessage message) {
        log.debug("Attempting to publish score event for eventId={}", message.eventId());
        var result = kafkaTemplate.send(topic, message.eventId(), message).join();
        log.info("Published score event for eventId={} to topic={}, offset={}",
                message.eventId(), topic, result.getRecordMetadata().offset());
    }

    @Recover
    public void publishRecover(Exception ex, ScoreEventMessage message) {
        log.error("Failed to publish score event for eventId={} after retries, sending to DLQ: {}",
                message.eventId(), ex.getMessage());
        dlqProducer.sendToDlq(message);
    }
}
