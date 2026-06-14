# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Local Development

### Option A — Scripts (recommended for running services locally without Docker)

```bash
# 1. Start infrastructure containers (Kafka, Redis, RabbitMQ, PostgreSQL instances)
./scripts/dev-up.sh           # Mac/Linux
./scripts/dev-up.ps1          # Windows

# 2. Run all services with the local Spring profile
./scripts/run-services.sh     # Mac/Linux
./scripts/run-services.ps1    # Windows

# 3. Check that all services are healthy
./scripts/health-check.sh

# 4. Stop infrastructure containers
./scripts/dev-down.sh
```

Logs are written to `logs/<service-name>.log` at the repo root.

### Option B — Full Docker Compose stack

```bash
# Full stack (infra + all application services, built from source)
docker compose -f platform/docker/docker-compose.yml up --build

# Infrastructure only (Kafka, Redis, RabbitMQ, all PostgreSQL instances)
docker compose -f platform/docker/docker-compose.dev.yml up
```

### Per-Service Maven

```bash
# From any service directory (e.g., services/order-service/)
mvn clean verify                          # Build + all tests
mvn clean package -DskipTests             # Build only
mvn test                                  # All tests
mvn test -Dtest=OrderServiceTest          # Single test class
mvn spring-boot:run -Dspring-boot.run.profiles=local   # Run with local profile
```

### Shared Library

Any change to `shared-libs/atlas-common-events` must be installed before rebuilding dependent services:

```bash
cd shared-libs/atlas-common-events && mvn clean install
```

### Kubernetes (local overlay)

```bash
kubectl apply -k platform/k8s/overlays/local
kubectl get pods -n atlas
kubectl port-forward -n atlas svc/order-service 8083:80
```

## Spring Profiles

| Profile  | Purpose                                                       |
|----------|---------------------------------------------------------------|
| `local`  | Runs against local Docker containers; uses `application-local.properties` |
| `docker` | Used inside Docker Compose; activates Docker hostnames        |
| `test`   | H2 in-memory DB for unit/integration tests                   |

## Architecture

**Atlas Commerce** is an event-driven microservices platform (Java 21, Spring Boot 3.x). Each service owns its own PostgreSQL database and communicates asynchronously via Kafka.

### Service Ports (local profile)

| Service               | Port |
|-----------------------|------|
| gateway-service       | 8080 |
| auth-service          | 8081 |
| catalog-service       | 8082 |
| order-service         | 8083 |
| inventory-service     | 8084 |
| cart-service          | 8085 |
| pricing-service       | 8086 |
| coupon-service        | 8087 |
| payment-service       | 8088 |
| shipping-service      | 8089 |
| notification-service  | 8091 |
| audit-service         | 8093 |

All services expose `/actuator/health`, `/actuator/health/liveness`, and `/actuator/health/readiness`.

### Saga Flow (Choreography — no central orchestrator)

```
ORDER_CREATED → INVENTORY_RESERVED → PAYMENT_COMPLETED → SHIPPING_CREATED → NOTIFICATION_SENT
```

Failure path: `INVENTORY_FAILED` → `ORDER_FAILED`. Failed events are also published to Kafka DLT topics.

Kafka topics: `order-events`, `inventory-events`, `payment-events`, `shipping-events`

### Resilience Strategy

Services publish to Kafka first. If Kafka is unavailable, Resilience4j circuit breakers fall back to RabbitMQ. The order-service continues Kafka publishing even when RabbitMQ is unavailable.

### Security

`auth-service` issues JWTs (access + refresh tokens, Redis blacklist for revocation). All other services are Spring Security OAuth2 Resource Servers — they validate tokens locally without calling `auth-service`. The gateway enforces JWT routing and Redis-backed rate limiting on public endpoints.

### Shared Library

`shared-libs/atlas-common-events` contains the Kafka event POJOs (`OrderCreatedEvent`, `InventoryReservedEvent`, etc.) shared across all services. Package: `com.atlascommerce.common.events`.

### Observability

- **Metrics**: Prometheus + Grafana (persistent storage)
- **Tracing**: OpenTelemetry + Tempo; `traceparent` propagated across all Kafka saga events
- **Correlation IDs**: injected at the gateway, propagated through headers

### Infrastructure (Docker container names)

| Container                | Role                        |
|--------------------------|-----------------------------|
| `atlas-kafka`            | Kafka 4.x (KRaft mode)      |
| `atlas-rabbitmq`         | RabbitMQ fallback           |
| `atlas-redis`            | JWT blacklist + cart        |
| `atlas-postgres-<svc>`   | Per-service PostgreSQL 17   |

### CI/CD

Each service has `.github/workflows/<service>-ci.yml` (Maven build → tests → Docker Hub push on pushes to master/main/develop). Kubernetes deployment via ArgoCD/Helm is planned but not yet wired.
