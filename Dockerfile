# Multi-stage build
FROM maven:3.9-eclipse-temurin-17-alpine as builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -Dmaven.test.skip=true

# Анализируем зависимости и создаем минимальный JAR
RUN jdeps --ignore-missing-deps \
          --multi-release 17 \
          --print-module-deps \
          --class-path target/dependency/* \
          target/*.jar > jre-deps.txt

# Создаем кастомную JRE
FROM eclipse-temurin:17-alpine as jre-builder
WORKDIR /app
COPY --from=builder /app/jre-deps.txt .
RUN $JAVA_HOME/bin/jlink \
    --add-modules $(cat jre-deps.txt),jdk.unsupported \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /minimal-jre

# Финальный образ
FROM alpine:3.18

WORKDIR /app

# Копируем минимальную JRE
COPY --from=jre-builder /minimal-jre /opt/java/
COPY --from=builder /app/target/*.jar app.jar

# Минимальные библиотеки
RUN apk add --no-cache libstdc++

# Non-root пользователь
RUN addgroup -S spring && adduser -S spring -G spring
RUN chown -R spring:spring /app
USER spring

ENV JAVA_HOME=/opt/java
ENV PATH="${JAVA_HOME}/bin:${PATH}"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
