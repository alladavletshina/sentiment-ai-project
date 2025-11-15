    package com.example.sentimentaiproject.config;

    import io.micrometer.core.instrument.Counter;
    import io.micrometer.core.instrument.Timer;
    import io.micrometer.core.instrument.Gauge;
    import io.micrometer.core.instrument.binder.MeterBinder;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;

    import java.util.concurrent.atomic.AtomicInteger;
    import java.util.concurrent.atomic.AtomicLong;

    /**
     * Конфигурация Spring Boot Actuator для мониторинга и метрик
     */
    @Configuration
    public class ActuatorConfig {

        // Счетчики для метрик
        private final AtomicInteger totalRequests = new AtomicInteger(0);
        private final AtomicInteger successfulRequests = new AtomicInteger(0);
        private final AtomicLong analysisDuration = new AtomicLong(0);

        /**
         * Кастомные метрики для приложения
         */
        @Bean
        public MeterBinder sentimentMetrics() {
            return registry -> {
                // Метрика для отслеживания количества анализов
                Gauge.builder("sentiment.analysis.requests.total", totalRequests, AtomicInteger::get)
                        .description("Общее количество запросов на анализ тональности")
                        .register(registry);

                // Метрика для успешных анализов
                Gauge.builder("sentiment.analysis.requests.successful", successfulRequests, AtomicInteger::get)
                        .description("Количество успешных анализов")
                        .register(registry);

                // Метрика для времени ответа
                Gauge.builder("sentiment.analysis.duration.milliseconds", analysisDuration, AtomicLong::get)
                        .description("Суммарное время выполнения анализов в миллисекундах")
                        .register(registry);

                // Counter для общего количества запросов
                Counter.builder("sentiment.analysis.counter.total")
                        .description("Счетчик общего количества запросов")
                        .register(registry);

                // Timer для времени выполнения
                Timer.builder("sentiment.analysis.timer")
                        .description("Таймер выполнения анализа тональности")
                        .register(registry);
            };
        }

        /**
         * Методы для обновления метрик
         */
        public void incrementTotalRequests() {
            totalRequests.incrementAndGet();
        }

        public void incrementSuccessfulRequests() {
            successfulRequests.incrementAndGet();
        }

        public void recordAnalysisDuration(long durationMs) {
            analysisDuration.addAndGet(durationMs);
        }

        /**
         * Получение текущих значений метрик
         */
        public int getTotalRequests() {
            return totalRequests.get();
        }

        public int getSuccessfulRequests() {
            return successfulRequests.get();
        }

        public long getAnalysisDuration() {
            return analysisDuration.get();
        }
    }