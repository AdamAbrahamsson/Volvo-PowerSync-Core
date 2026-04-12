# ⚡ Volvo PowerSync Core

Event-driven **EV charging** demo: a multi-module **Java / Spring Boot** stack that simulates a fleet, books chargers, and streams live status to a web dashboard.

### Hosted demo

A public instance may be available at **[powersync.adamcloud.dev](https://powersync.adamcloud.dev/)** when the deployment is running. It is backed by a **Raspberry Pi 5** that is **not always on**—if the site is down, ask the **repository owner** to power the hardware on and start the stack.

Deployment on the Pi cluster (Kubernetes, tunnel, images, and related ops) is documented in the companion repo **[Volvo-PowerSync-Infra-Pi5](https://github.com/adamabrahamsson/Volvo-PowerSync-Infra-Pi5)**.

---

## System overview

Below you can see a diagram of the system architecture.

(./volvo-powersync-diagram.png)

---

## Modules

| Module | Role |
|--------|------|
| `booking-api` | gRPC **proto** contract shared by services |
| `booking-service` | Station book/release, **PostgreSQL** + **Flyway**, **gRPC** server + VIP **REST**, **Kafka** in/out |
| `simulator-service` | In-memory **car fleet**, normal flow via **gRPC**, VIP flow via **Kafka**, publishes car telemetry |
| `notification-service` | Consumes **Kafka**, pushes live updates to the UI via **SSE** |
| `frontend` | Static **dashboard** + VIP driver chooser |

---

## High-level flow

- **Normal cars** — When battery is low (`≤ 20%`), the simulator books a **normal** station over **gRPC**; when charged (`≥ 80%`), it releases the slot (with **VIN-based** release if ids drift).
- **VIP cars** (`EC40` / `ES90`) — Booked manually from the **frontend**; flow is **Kafka**-mediated:
  - Booking publishes **VIP booked** → simulator starts charging that car  
  - At charge target, simulator publishes **VIP charging completed** → booking frees the VIP station  
- **Live UI** — **Notification** service streams station + car updates to the browser via **SSE** (fed by Kafka from simulator & booking).

---

## Tech stack

- Java **21**, Spring Boot **3**
- Maven multi-module build
- PostgreSQL **16**
- Flyway migrations
- **gRPC** (booking ↔ simulator, normal charging flow)
- **Apache Kafka**
- **SSE** (live dashboard updates)

---

## Prerequisites

- **JDK 21+**
- **Maven 3.9+**
- **Docker** + **Docker Compose**

---

## Run locally

**1.** Start infra (Postgres + Kafka):

```bash
docker compose up -d
```

**2.** Start each service in its own terminal (from repo root):

```bash
mvn -pl booking-service -am spring-boot:run -DskipTests
```

```bash
mvn -pl simulator-service -am spring-boot:run -DskipTests
```

```bash
mvn -pl notification-service -am spring-boot:run -DskipTests
```

**3.** Serve the frontend:

```bash
cd frontend
python3 -m http.server 5173
```

**4.** Open in the browser:

| Page | URL |
|------|-----|
| Driver chooser | [http://localhost:5173/choose-driver.html](http://localhost:5173/choose-driver.html) |
| Dashboard | [http://localhost:5173/index.html](http://localhost:5173/index.html) |

---

## Default ports

| Service | Port |
|---------|------|
| Booking REST | **8081** |
| Booking gRPC | **9090** |
| Simulator REST | **8080** |
| Notification REST / SSE | **8082** |
| PostgreSQL | **5432** |
| Kafka | **9092** |

---

## Key notification API (frontend)

Used for live dashboard data (paths on **notification-service**):

| | Endpoint |
|--|----------|
| Station snapshot | `GET /api/stations-status` |
| Station live stream | `GET /api/stations-status/stream` · event: `station-status` |
| Car snapshot | `GET /api/cars-status` |
| Car live stream | `GET /api/cars-status/stream` · event: `car-status` |

The UI also calls **simulator** `GET /api/cars` for fleet metadata and **booking** `POST /api/vip-stations/book` for VIP booking.

---

## Kafka topics

| Topic | Role | Producer → Consumer | Payload (JSON) |
|-------|------|---------------------|----------------|
| `vip-booked-events` | VIP slot reserved | **booking** → **simulator** | `vin`, `chargingStationId`, `timestampMs` |
| `vip-charging-completed-events` | VIP finished charging | **simulator** → **booking** | `vin`, `chargingStationId`, `timestampMs` |
| `station-status-events` | Charger row updates | **booking** → **notification** | `stationId`, `stationName`, `stationType`, `status`, `assignedVin`, `timestampMs` |
| `car-status-events` | Fleet telemetry each tick | **simulator** → **notification** | `vin`, `batteryPercentage`, `status`, `assignedChargingStationId`, `vipEligible`, `timestampMs` |
