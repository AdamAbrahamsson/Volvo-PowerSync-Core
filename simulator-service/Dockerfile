FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY pom.xml ./
COPY booking-api ./booking-api
COPY booking-service ./booking-service
COPY notification-service ./notification-service
COPY simulator-service ./simulator-service

RUN mvn -B -pl simulator-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /workspace/simulator-service/target/simulator-service-*.jar /app/simulator-service.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/simulator-service.jar"]
