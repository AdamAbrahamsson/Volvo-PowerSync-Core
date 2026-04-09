FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Copy only required modules for this build.
COPY pom.xml ./
COPY booking-api ./booking-api
COPY booking-service ./booking-service
COPY notification-service ./notification-service
COPY simulator-service ./simulator-service

RUN mvn -B -pl booking-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /workspace/booking-service/target/booking-service-*.jar /app/booking-service.jar

EXPOSE 8081 9090

ENTRYPOINT ["java", "-jar", "/app/booking-service.jar"]
