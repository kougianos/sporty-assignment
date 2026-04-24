package com.sporty.tracker.service;

import com.sporty.tracker.dto.ScoreEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DlqConsumer {

    private final ScoreEventProducer scoreEventProducer;

    @KafkaListener(topics = "${app.kafka.dlq-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeDlqMessage(ScoreEventMessage message) {
        log.info("DLQ: Reprocessing failed message for eventId={}", message.eventId());
        scoreEventProducer.publish(message);
    }
}
