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
chmod +x ./scripts/dev/dev-up.sh
./scripts/dev/dev-up.sh
```

## Run Microservices

### Windows

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-services.ps1
```

### Linux

```bash
chmod +x ./scripts/run-services.sh
./scripts/dev/run-services.sh
```

## Verify Platform Health

### Windows

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\health-check.ps1
```

### Linux

```bash
chmod +x ./scripts/dev/health-check.sh
./scripts/dev/health-check.sh
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
chmod +x ./scripts/dev/dev-stop-services.sh
./scripts/dev/dev-stop-services.sh
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

./scripts/dev/dev-up.sh

./scripts/dev/run-services.sh

./scripts/dev/health-check.sh
```

### Shutdown

```bash
./scripts/dev/dev-stop-services.sh

docker compose -f ./platform/docker/docker-compose.dev.yml down
```

## Local Kubernetes deployment with Helm

Atlas Commerce includes a modular Helm chart for deploying the core platform services locally on Minikube.

### Current Helm components

The local Helm deployment currently includes:

```text
auth-service
gateway-service
catalog-service
redis
kafka
postgres-auth
postgres-catalog
```

The Helm chart is located at:

```text
platform/helm/atlas-commerce
```

The local namespace used by default is:

```text
atlas-helm
```

### Helm values structure

The chart uses separate values files per component:

```text
platform/helm/atlas-commerce/values/
├── auth.yaml
├── catalog.yaml
├── gateway.yaml
├── kafka.yaml
├── postgres.yaml
├── redis.yaml
└── secrets-local.yaml
```

Important:

```text
secrets-local.yaml must not be committed to Git.
```

It contains local secrets such as JWT secrets and database passwords.

### Helm helper scripts

To avoid running long Helm commands manually, use the helper scripts located in:

```text
scripts/helm/
```

Available scripts:

```text
template-local.sh
dry-run-local.sh
upgrade-local.sh
status-local.sh

template-local.ps1
dry-run-local.ps1
upgrade-local.ps1
status-local.ps1
```

### Linux / Git Bash usage

Render the Helm manifests:

```bash
./scripts/helm/template-local.sh
```

Run a Helm dry-run:

```bash
./scripts/helm/dry-run-local.sh
```

Install or upgrade the local release:

```bash
./scripts/helm/upgrade-local.sh
```

Check Helm, pods, services and PVCs:

```bash
./scripts/helm/status-local.sh
```

### Windows PowerShell usage

PowerShell may block `.ps1` execution by default. To allow scripts only in the current terminal session, run:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

Then run:

```powershell
.\scripts\helm\template-local.ps1
.\scripts\helm\dry-run-local.ps1
.\scripts\helm\upgrade-local.ps1
.\scripts\helm\status-local.ps1
```

Alternatively, run a script directly with bypass:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\helm\template-local.ps1
```

### Validate the local deployment

Check pods:

```bash
kubectl get pods -n atlas-helm
```

Check services:

```bash
kubectl get svc -n atlas-helm
```

Check persistent volumes:

```bash
kubectl get pvc -n atlas-helm
```

Check Helm status:

```bash
helm status atlas -n atlas-helm
```

### Gateway port-forward

Expose the API Gateway locally:

```bash
kubectl port-forward -n atlas-helm svc/gateway-service 8080:80
```

Then test:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/auth/v3/api-docs
curl http://localhost:8080/catalog/v3/api-docs
```

Expected result:

```text
Gateway health: UP
Auth OpenAPI reachable through Gateway
Catalog OpenAPI reachable through Gateway
```

### Logs

Check service logs:

```bash
kubectl logs -n atlas-helm deploy/gateway --tail=50
kubectl logs -n atlas-helm deploy/auth --tail=50
kubectl logs -n atlas-helm deploy/catalog --tail=50
kubectl logs -n atlas-helm deploy/kafka --tail=50
```

Check that local Gateway is not trying to export traces to Tempo:

```bash
kubectl logs -n atlas-helm deploy/gateway --since=2m | grep -iE "tempo|otlp|failed to export"
```

No output means the local OTLP/Tempo export is disabled correctly.

