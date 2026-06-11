# Atlas Commerce - Developer Experience

## Purpose

Run Atlas locally without requiring Kubernetes.

The local workflow is:

```text
Docker Compose → Dependencies
VS Code / Scripts → Microservices
Health Check → Validation
```

## Requirements

Install the following tools:

* Java 21
* Maven 3.9+
* Docker Desktop
* Git Bash or PowerShell
* Visual Studio Code

Verify your installation:

```bash
java -version
mvn -version
docker version
docker compose version
```

## Start Infrastructure

From the project root:

### Windows

```powershell
docker compose -f .\platform\docker\docker-compose.dev.yml up -d
```

### Linux / Git Bash

```bash
docker compose -f ./platform/docker/docker-compose.dev.yml up -d
```

This will start:

```text
PostgreSQL
Redis
Kafka
RabbitMQ
```

## Create Kafka Topics

### Windows

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\dev-up.ps1
```

### Linux
```bash
chmod +x ./scripts/dev-up.sh
./scripts/dev-up.sh
```

## Run Microservices

### Windows

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-services.ps1
```

### Linux

```bash
chmod +x ./scripts/run-services.sh
./scripts/run-services.sh
```

## Verify Platform Health

### Windows

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\health-check.ps1
```

### Linux

```bash
chmod +x ./scripts/health-check.sh
./scripts/health-check.sh
```

Expected output:

```text
[UP] gateway
[UP] auth
[UP] catalog
[UP] order
[UP] inventory
[UP] cart
[UP] pricing
[UP] coupon
[UP] payment
[UP] shipping
[UP] notification
[UP] audit
```

## Stop Microservices

### Windows

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\dev-stop-services.ps1
```

### Linux

```bash
chmod +x ./scripts/dev-stop-services.sh
./scripts/dev-stop-services.sh
```

## Stop Infrastructure

### Windows

```powershell
docker compose -f .\platform\docker\docker-compose.dev.yml down
```

### Linux

```bash
docker compose -f ./platform/docker/docker-compose.dev.yml down
```

## Full Environment Reset

To remove containers, networks, and volumes:

```powershell
docker compose -f .\platform\docker\docker-compose.dev.yml down -v
```

## Microservice Ports

| Service      | Port |
| ------------ | ---- |
| gateway      | 8080 |
| auth         | 8081 |
| catalog      | 8082 |
| order        | 8083 |
| inventory    | 8084 |
| cart         | 8085 |
| pricing      | 8086 |
| coupon       | 8087 |
| payment      | 8088 |
| shipping     | 8089 |
| notification | 8091 |
| audit        | 8093 |

## Local Databases

| Service      | Port | Database       |
| ------------ | ---- | -------------- |
| auth         | 5432 | authdb         |
| catalog      | 5433 | catalogdb      |
| inventory    | 5434 | inventorydb    |
| order        | 5435 | orderdb        |
| payment      | 5436 | paymentdb      |
| shipping     | 5437 | shippingdb     |
| notification | 5438 | notificationdb |
| audit        | 5439 | auditdb        |
| coupon       | 5440 | coupondb       |
| cart         | 5441 | cartdb         |

Credentials:

```text
Username: atlas
Password: atlas
```

## Useful URLs

```text
Gateway Health:
http://localhost:8080/actuator/health

Gateway Swagger UI:
http://localhost:8080/swagger-ui.html

RabbitMQ Management UI:
http://localhost:15672

Username: atlas
Password: atlas
```

## Local Profile

All microservices use:

```text
SPRING_PROFILES_ACTIVE=local
```

Each service contains:

```text
application-local.properties
```

or, for the gateway:

```text
application-local.yml
```

## Local Observability

By default, local development runs without the full observability stack.

```text
OpenTelemetry
Tempo
Prometheus
Grafana
Loki
```

General rule:

```text
local = fast development
kubernetes = full integration
production = full observability
```

## Recommended Workflow

### Startup

```bash
docker compose -f ./platform/docker/docker-compose.dev.yml up -d

./scripts/dev-up.sh

./scripts/run-services.sh

./scripts/health-check.sh
```

### Shutdown

```bash
./scripts/dev-stop-services.sh

docker compose -f ./platform/docker/docker-compose.dev.yml down
```

## Architecture Overview

Atlas Commerce is a cloud-native e-commerce platform built to demonstrate modern software engineering and DevOps practices.

Core technologies include:

* Spring Boot
* PostgreSQL
* Redis
* Apache Kafka
* RabbitMQ
* Docker
* Kubernetes
* Helm
* ArgoCD
* Prometheus
* Grafana
* OpenTelemetry
* AWS EKS

The project is designed to support both local development and cloud-native deployments while maintaining a consistent developer experience across environments.
