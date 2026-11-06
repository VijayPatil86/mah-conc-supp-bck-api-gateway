package com.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.observation.ClientRequestObservationContext;

import io.micrometer.observation.ObservationPredicate;

@Configuration
public class TracingConfig {

    @Bean
    ObservationPredicate ignoreEurekaCalls() {
        return (name, context) -> {
            if (context instanceof ClientRequestObservationContext client
                    && client.getCarrier() != null) {
                String path = client.getCarrier().getURI().getPath();
                return path == null || !path.startsWith("/eureka");
            }
            return true;                      // everything else is still traced
        };
    }
}