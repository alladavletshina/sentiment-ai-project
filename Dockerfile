# Multi-stage build для минимального образа
FROM maven:3.9-eclipse-temurin-17-alpine as builder

WORKDIR /app

# Копирование только pom.xml сначала для кэширования зависимостей
COPY pom.xml .

# Скачивание зависимостей
RUN mvn dependency:go-offline -B

# Копирование исходного кода и сборка
COPY src ./src
RUN mvn clean package -DskipTests -Dmaven.test.skip=true

# Финальный образ на основе ultra-slim JRE
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Создание non-root пользователя
RUN addgroup -S spring && adduser -S spring -G spring

# Копирование JAR из builder stage
COPY --from=builder /app/target/sentiment-ai-app-*.jar app.jar

# Настройка прав
RUN chown -R spring:spring /app
USER spring

# Health check с использованием wget (в Alpine нет curl)
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# Оптимизация JVM для контейнеров
ENTRYPOINT ["java", "-jar", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-Xss512k", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "app.jar"]