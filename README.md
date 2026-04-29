# LinkMe API Gateway

A production-ready API Gateway for the **LinkMe** microservices ecosystem, built with **Spring Cloud Gateway** and **Spring Boot 3**. It serves as the single entry point for all client requests — handling JWT authentication, request routing, circuit breaking, and contract validation before traffic reaches downstream services.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Environment Variables](#environment-variables)
  - [Running Locally](#running-locally)
  - [Running with Docker Compose](#running-with-docker-compose)
- [Service Routes](#service-routes)
- [Health & Monitoring](#health--monitoring)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Author](#author)

---

## Overview

The LinkMe API Gateway acts as the central hub for all microservice communication within the LinkMe platform. Rather than clients communicating with individual services directly, all traffic is routed through this gateway — where it is authenticated, validated, and forwarded to the appropriate service.

---

## Features

- **JWT Authentication** — Stateless token validation on every incoming request using `jjwt`
- **Reactive Routing** — Non-blocking request forwarding with Spring Cloud Gateway (WebFlux)
- **Circuit Breaker** — Resilience4j integration to prevent cascading failures across services
- **Swagger Contract Validation** — OpenAPI-based request/response validation using `swagger-request-validator`
- **Spring Cloud Contract** — Consumer-driven contract testing with WireMock stub runner
- **Spring Security** — Security filter chain configured for gateway-layer enforcement
- **Actuator Health Checks** — Live readiness and health endpoints for container orchestration
- **Dockerised** — Multi-stage Docker build with a non-root user for security best practices
- **Docker Compose** — Full local stack including Auth Service, User Service, PostgreSQL, and Redis

---

## Architecture

```
Client
  │
  ▼
┌─────────────────────────┐
│   LinkMe API Gateway    │  ← JWT validation, routing, circuit breaking
│        :8080            │
└────────────┬────────────┘
             │
     ┌───────┴────────┐
     ▼                ▼
Auth Service     User Service
   :8081             :8082
     │
  PostgreSQL + Redis
```

All services communicate over an isolated Docker bridge network (`linkme-network`).

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.4 |
| Gateway | Spring Cloud Gateway (Reactive) |
| Security | Spring Security + JJWT 0.12.7 |
| Resilience | Resilience4j (Circuit Breaker) |
| Contract Testing | Spring Cloud Contract + WireMock |
| API Validation | Atlassian Swagger Request Validator |
| Monitoring | Spring Boot Actuator |
| Build Tool | Maven 3.9.9 |
| Containerisation | Docker (multi-stage) + Docker Compose |
| Boilerplate | Lombok |

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose

### Environment Variables

Create a `.env` file in the project root. The following variables are required:

```env
SPRING_SERVER_PORT=8080

# JWT
MY_SECRET_KEY=your_jwt_secret_here

# Database
SPRING_DATABASE_NAME=linkme
SPRING_DATABASE_USERNAME=postgres
SPRING_DATABASE_PASSWORD=your_db_password

# Service URLs (used internally in Docker)
AUTH_SERVICE_ROUTES=http://auth-service:8081
PROTECTED_SERVICE_ROUTES=http://user-service:8082
```

> **Never commit your `.env` file.** It is already listed in `.gitignore`.

### Running Locally

```bash
# Clone the repository
git clone https://github.com/Likeabishop/LinkMeAPIGateway.git
cd LinkMeAPIGateway

# Build the project
./mvnw clean package -DskipTests

# Run the application
./mvnw spring-boot:run
```

The gateway will be available at `http://localhost:8080`.

### Running with Docker Compose

```bash
# Build and start all services
docker compose up --build

# Run in detached mode
docker compose up --build -d

# Stop all services
docker compose down
```

This will spin up:
- `linkme-gateway` — the API gateway on the port defined by `SPRING_SERVER_PORT`
- `auth-service` — authentication microservice on port `8081`
- `user-service` — user management microservice on port `8082`
- `postgres` — PostgreSQL 15 database on port `5433`
- `redis` — Redis 7 cache on port `6380`

---

## Service Routes

| Route | Upstream Service | Port |
|---|---|---|
| `/auth/**` | Auth Service | 8081 |
| `/api/**` | User Service | 8082 |

Routes are configured in `application.yml` (or `application.properties`) and resolved internally within the Docker network.

---

## Health & Monitoring

Spring Boot Actuator exposes health endpoints used by Docker and any orchestration layer (e.g. Kubernetes):

```
GET http://localhost:8080/actuator/health
```

The Dockerfile also includes a built-in `HEALTHCHECK` that polls this endpoint every 30 seconds.

---

## Testing

The project uses a layered testing strategy:

```bash
# Run unit tests
./mvnw test

# Run integration tests (includes WireMock contract stubs)
./mvnw verify
```

- **Unit tests** — Standard Spring Boot test slice tests
- **Integration tests** — WireMock stubs validate Swagger contract compliance end-to-end
- **Reactor tests** — Reactive stream assertions via `reactor-test`

---

## Project Structure

```
LinkMeAPIGateway/
├── src/
│   ├── main/
│   │   ├── java/com/example/LinkMeApiGateway/
│   │   │   ├── config/         # Security & gateway route configuration
│   │   │   ├── security/         # JWT authentication filters
│   │   │   └── ...
│   │   └── resources/
│   │       └── application.yml # Route definitions & app config
│   └── test/
│       └── java/               # Unit & integration tests
├── Dockerfile                  # Multi-stage Docker build
├── compose.yaml                # Full local stack definition
├── pom.xml                     # Maven dependencies
└── .env                        # Local environment variables (not committed)
```

---

## Author

**Karabo Tebeila**

- Portfolio: [justkarabo.xyz](https://www.justkarabo.xyz)
- GitHub: [@Likeabishop](https://github.com/Likeabishop)
- LinkedIn: [karabo-tebeila](https://www.linkedin.com/in/karabo-tebeila-90881331b)
- Email: karabotebeila24@gmail.com
