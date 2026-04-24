package com.sporty.tracker;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class TestKafkaHelper {

    private TestKafkaHelper() {
    }

    public static void configureKafkaProperties(DynamicPropertyRegistry registry, KafkaContainer kafka) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    public static <T> KafkaConsumer<String, T> createConsumer(KafkaContainer kafka, String topic) {
        var props = Map.<String, Object>of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + topic,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class,
                JsonDeserializer.TRUSTED_PACKAGES, "*"
        );

        var consumer = new KafkaConsumer<String, T>(props);
        consumer.subscribe(List.of(topic));
        return consumer;
    }

    public static <T> ConsumerRecords<String, T> pollUntilRecords(KafkaConsumer<String, T> consumer, Duration timeout) {
        var deadline = System.currentTimeMillis() + timeout.toMillis();
        ConsumerRecords<String, T> records;
        do {
            records = consumer.poll(Duration.ofMillis(500));
            if (!records.isEmpty()) {
                return records;
            }
        } while (System.currentTimeMillis() < deadline);
        return records;
    }

    public static <T> void produce(KafkaContainer kafka, String topic, String key, T value) {
        var props = Map.<String, Object>of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
        );

        try (var producer = new KafkaProducer<String, T>(props)) {
            producer.send(new ProducerRecord<>(topic, key, value)).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to produce test message", e);
        }
    }
}
