package com.diagnostician.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class ObservabilityConfig {

    @Bean
    public AtomicLong aiRequestCounter(MeterRegistry registry) {
        AtomicLong counter = new AtomicLong(0);
        registry.gauge("ai.requests.total", counter, AtomicLong::get);
        return counter;
    }
}
