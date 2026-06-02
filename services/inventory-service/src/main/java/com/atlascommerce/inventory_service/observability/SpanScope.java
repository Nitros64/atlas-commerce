package com.atlascommerce.inventory_service.observability;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;

public record SpanScope(Span span, Scope scope) implements AutoCloseable {
    @Override
    public void close() {
        scope.close();
        span.end();
    }
}