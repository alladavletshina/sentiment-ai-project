# Multi-stage build для минимального образа
FROM openjdk:21-jdk-slim as builder

WORKDIR /app

# Копирование исходного кода
COPY pom.xml .
COPY src ./src

# Установка Maven и сборка
RUN apt-get update && apt-get install -y maven
RUN mvn clean package -DskipTests

# Финальный образ
FROM openjdk:21-jdk-slim

WORKDIR /app

# Создание non-root пользователя
RUN groupadd -r spring && useradd -r -g spring spring

# Копирование JAR из builder stage
COPY --from=builder /app/target/sentiment-ai-app-1.0.0.jar app.jar

# Настройка прав
RUN chown -R spring:spring /app
USER spring

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]