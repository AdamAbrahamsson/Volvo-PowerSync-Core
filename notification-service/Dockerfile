FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Copy modules required by the parent reactor and notification build.
COPY pom.xml ./
COPY booking-api ./booking-api
COPY booking-service ./booking-service
COPY notification-service ./notification-service
COPY simulator-service ./simulator-service

RUN mvn -B -pl notification-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /workspace/notification-service/target/notification-service-*.jar /app/notification-service.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/notification-service.jar"]
