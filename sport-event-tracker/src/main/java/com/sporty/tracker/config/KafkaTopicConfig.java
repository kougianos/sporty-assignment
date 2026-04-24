package com.sporty.tracker.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topic}")
    private String scoreTopic;

    @Value("${app.kafka.dlq-topic}")
    private String dlqTopic;

    @Bean
    public NewTopic scoresTopic() {
        return TopicBuilder.name(scoreTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic scoresDlqTopic() {
        return TopicBuilder.name(dlqTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
