package com.atlascommerce.order.messaging.kafka;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.atlascommerce.order.event.OrderCreatedEvent;
import com.atlascommerce.order.event.OrderEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.tracing.Tracer;

@Component
@Qualifier("kafkaOrderEventPublisher")
@Slf4j
@RequiredArgsConstructor
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private final Tracer tracer;

    @Value("${atlas.kafka.topics.order-events}")
    private String orderEventsTopic;

    @Override
    @CircuitBreaker(name = "orderKafkaPublisher", fallbackMethod = "fallbackPublish")
    @Retry(name = "orderKafkaPublisher")
    public void publishOrderCreated(OrderCreatedEvent event) {
        try {

            String payload = objectMapper.writeValueAsString(event);

            ProducerRecord<String, String> record =
                    new ProducerRecord<>(
                            orderEventsTopic,
                            String.valueOf(event.orderId()),
                            payload
                    );

            var currentSpan = tracer.currentSpan();

            if (currentSpan != null) {
                String traceId = currentSpan.context().traceId();
                String spanId = currentSpan.context().spanId();

                String traceparent = "00-" + traceId + "-" + spanId + "-01";

                record.headers().add(
                        "traceparent",
                        traceparent.getBytes(StandardCharsets.UTF_8)
                );
            }

            kafkaTemplate.send(record).get(3, TimeUnit.SECONDS);

            log.info("Published ORDER_CREATED orderId={}", event.orderId());

        } catch (Exception e) {

            throw new RuntimeException("Failed to publish event", e);
        }
    }

    @SuppressWarnings("unused")
    private void fallbackPublish(OrderCreatedEvent event, Throwable ex) {

        log.error(
                "ORDER_CREATED Kafka fallback. orderId={} error={}",
                event.orderId(),
                ex.getMessage()
        );
    }
}