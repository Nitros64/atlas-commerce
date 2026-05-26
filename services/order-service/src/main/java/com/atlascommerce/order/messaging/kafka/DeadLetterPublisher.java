package com.atlascommerce.order.messaging.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publish(String dltTopic, String key, String payload, Exception exception) {

        kafkaTemplate.send(dltTopic, key, payload);

        log.error(
                "DLT_PUBLISHED topic={} key={} error={}",
                dltTopic,
                key,
                exception.getMessage()
        );
    }
}