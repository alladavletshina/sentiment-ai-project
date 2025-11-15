package com.example.sentimentaiproject.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class ActuatorConfig {

    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger successfulRequests = new AtomicInteger(0);
    private final AtomicLong analysisDuration = new AtomicLong(0);
    private final Counter requestCounter;

    public ActuatorConfig(MeterRegistry registry) {

        this.requestCounter = Counter.builder("sentiment.analysis.counter.total")
                .description("Счетчик общего количества запросов")
                .register(registry);
    }

    public void incrementTotalRequests() {
        totalRequests.incrementAndGet();
        requestCounter.increment();
    }

    public void incrementSuccessfulRequests() {
        successfulRequests.incrementAndGet();
    }

    public void addAnalysisDuration(long durationMs) {
        analysisDuration.addAndGet(durationMs);
    }

    @Bean
    public MeterBinder sentimentMetrics() {
        return registry -> {
            Gauge.builder("sentiment.analysis.requests.total", totalRequests, AtomicInteger::get)
                    .description("Общее количество запросов на анализ тональности")
                    .register(registry);

            Gauge.builder("sentiment.analysis.requests.successful", successfulRequests, AtomicInteger::get)
                    .description("Количество успешных анализов")
                    .register(registry);

            Gauge.builder("sentiment.analysis.duration.milliseconds", analysisDuration, AtomicLong::get)
                    .description("Суммарное время выполнения анализов в миллисекундах")
                    .register(registry);

            Counter.builder("sentiment.analysis.counter.total")
                    .description("Счетчик общего количества запросов")
                    .register(registry);

            Timer.builder("sentiment.analysis.timer")
                    .description("Таймер выполнения анализа тональности")
                    .register(registry);
        };
    }
}