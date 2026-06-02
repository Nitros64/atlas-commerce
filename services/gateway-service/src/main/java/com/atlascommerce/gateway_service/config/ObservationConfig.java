package com.atlascommerce.gateway_service.config;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;

@Configuration
public class ObservationConfig {

    @Bean
    ObservationPredicate skipActuatorObservations() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext serverContext) {

                String path = serverContext
                                .getCarrier()
                                .getPath()
                                .value();

                return !path.startsWith("/actuator");
            }

            return true;
        };
    }
}