package com.atlascommerce.payment_service.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaTracingHelper {

    private final OpenTelemetry openTelemetry;

    public SpanScope startConsumerSpan(ConsumerRecord<String, String> record, String spanName) {
        Tracer tracer = openTelemetry.getTracer("atlas-inventory-kafka");

        Context parentContext = openTelemetry
                .getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), record, getter());

        var span = tracer.spanBuilder(spanName)
                .setParent(parentContext)
                .setSpanKind(SpanKind.CONSUMER)
                .setAttribute("messaging.system", "kafka")
                .setAttribute("messaging.destination.name", record.topic())
                .setAttribute("messaging.kafka.partition", record.partition())
                .setAttribute("messaging.kafka.offset", record.offset())
                .startSpan();

        Scope scope = span.makeCurrent();

        return new SpanScope(span, scope);
    }

    private TextMapGetter<ConsumerRecord<String, String>> getter() {
        return new TextMapGetter<>() {

            @Override
            public Iterable<String> keys(ConsumerRecord<String, String> carrier) {
                List<String> keys = new ArrayList<>();
                carrier.headers().forEach(header -> keys.add(header.key()));
                return keys;
            }

            @Override
            public String get(
                    ConsumerRecord<String, String> carrier,
                    String key) {

                var header = carrier.headers().lastHeader(key);

                if (header == null) {
                    return null;
                }

                return new String(header.value(), StandardCharsets.UTF_8);
            }
        };
    }
}