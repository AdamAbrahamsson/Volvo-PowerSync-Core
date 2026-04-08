# Volvo PowerSync Core

Event-driven EV charging simulator built as a multi-module Java/Spring project.

## Modules

- `booking-api`: gRPC proto contract shared by services.
- `booking-service`: station booking/release logic, PostgreSQL + Flyway, gRPC server + VIP REST endpoint, Kafka producer/consumer.
- `simulator-service`: in-memory car fleet simulation, normal flow via gRPC, VIP flow via Kafka, car status Kafka publisher.
- `notification-service`: consumes Kafka events and pushes live updates to frontend via SSE.
- `frontend`: static dashboard + VIP driver chooser page.

## High-Level Flow

- Normal cars auto-book a normal station when battery is low (`<= 20%`).
- VIP cars (`EC40`/`ES90`) are manually booked from frontend.
- VIP booking uses Kafka between services:
  - booking publishes VIP booked event
  - simulator starts charging selected VIP car
  - simulator publishes VIP charging completed event at `>= 80%`
  - booking releases station
- Notification service streams live station and car updates to the frontend via SSE.

## Tech Stack

- Java 21, Spring Boot 3
- Maven multi-module build
- PostgreSQL 16
- Flyway migrations
- gRPC (booking-service <-> simulator-service for normal flow)
- Kafka (KRaft mode)
- SSE for live UI updates

## Prerequisites

- JDK 21+
- Maven 3.9+
- Docker + Docker Compose

## Run Locally

From repository root:

1) Start infra:

```bash
docker compose up -d
```

2) Start services (separate terminals):

```bash
mvn -pl booking-service -am spring-boot:run -DskipTests
```

```bash
mvn -pl simulator-service -am spring-boot:run -DskipTests
```

```bash
mvn -pl notification-service -am spring-boot:run -DskipTests
```

3) Run frontend:

```bash
cd frontend
python3 -m http.server 5173
```

Open:

- Driver chooser: [http://localhost:5173/choose-driver.html](http://localhost:5173/choose-driver.html)
- Dashboard: [http://localhost:5173/index.html](http://localhost:5173/index.html)

## Default Ports

- Booking REST: `8081`
- Booking gRPC: `9090`
- Simulator REST: `8080`
- Notification REST/SSE: `8082`
- PostgreSQL: `5432`
- Kafka: `9092`

## Key Frontend Endpoints

- Station snapshot: `GET /api/stations-status`
- Station SSE: `GET /api/stations-status/stream` (event: `station-status`)
- Car snapshot: `GET /api/cars-status`
- Car SSE: `GET /api/cars-status/stream` (event: `car-status`)
