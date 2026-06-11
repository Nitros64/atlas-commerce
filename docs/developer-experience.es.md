# Atlas Commerce - Experiencia de Desarrollo

## Objetivo

Ejecutar Atlas localmente sin depender de Kubernetes.

El flujo local es:

```text
Docker Compose → dependencias
VS Code / scripts → microservicios
health-check → validación
```

## Requisitos

Instalar:

* Java 21
* Maven 3.9+
* Docker Desktop
* Git Bash o PowerShell
* Visual Studio Code

Verificar instalación:

```bash
java -version
mvn -version
docker version
docker compose version
```

## Levantar dependencias

Desde la raíz del proyecto:

### Windows

```powershell
docker compose -f .\platform\docker\docker-compose.dev.yml up -d
```

### Linux / Git Bash

```bash
docker compose -f ./platform/docker/docker-compose.dev.yml up -d
```

Esto iniciará:

```text
PostgreSQL
Redis
Kafka
RabbitMQ
```

## Crear Topics de Kafka

### Windows

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\dev-up.ps1
```

### Linux
```bash
chmod +x ./scripts/dev-up.sh
./scripts/dev-up.sh
```

## Ejecutar los microservicios

### Windows

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-services.ps1
```

### Linux

```bash
chmod +x ./scripts/run-services.sh
./scripts/run-services.sh
```

## Verificar el estado de la plataforma

### Windows

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\health-check.ps1
```

### Linux

```bash
chmod +x ./scripts/health-check.sh
./scripts/health-check.sh
```

Resultado esperado:

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

## Detener los microservicios

### Windows

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\dev-stop-services.ps1
```

### Linux

```bash
chmod +x ./scripts/dev-stop-services.sh
./scripts/dev-stop-services.sh
```

## Detener la infraestructura

### Windows

```powershell
docker compose -f .\platform\docker\docker-compose.dev.yml down
```

### Linux

```bash
docker compose -f ./platform/docker/docker-compose.dev.yml down
```

## Reinicio completo del entorno

Para eliminar también los volúmenes y datos persistentes:

```powershell
docker compose -f .\platform\docker\docker-compose.dev.yml down -v
```

## Puertos de los microservicios

| Servicio     | Puerto |
| ------------ | ------ |
| gateway      | 8080   |
| auth         | 8081   |
| catalog      | 8082   |
| order        | 8083   |
| inventory    | 8084   |
| cart         | 8085   |
| pricing      | 8086   |
| coupon       | 8087   |
| payment      | 8088   |
| shipping     | 8089   |
| notification | 8091   |
| audit        | 8093   |

## Bases de datos locales

| Servicio     | Puerto | Base de Datos  |
| ------------ | ------ | -------------- |
| auth         | 5432   | authdb         |
| catalog      | 5433   | catalogdb      |
| inventory    | 5434   | inventorydb    |
| order        | 5435   | orderdb        |
| payment      | 5436   | paymentdb      |
| shipping     | 5437   | shippingdb     |
| notification | 5438   | notificationdb |
| audit        | 5439   | auditdb        |
| coupon       | 5440   | coupondb       |
| cart         | 5441   | cartdb         |

Credenciales:

```text
usuario: atlas
contraseña: atlas
```

## URLs útiles

```text
Health Gateway:
http://localhost:8080/actuator/health

Swagger Gateway:
http://localhost:8080/swagger-ui.html

RabbitMQ UI:
http://localhost:15672
usuario: atlas
contraseña: atlas
```

## Perfil Local

Todos los microservicios utilizan:

```text
SPRING_PROFILES_ACTIVE=local
```

Cada servicio dispone de:

```text
application-local.properties
```

o en el caso del gateway:

```text
application-local.yml
```

## Observabilidad Local

Por defecto, en modo local se ejecuta sin toda la plataforma de observabilidad.

```text
OpenTelemetry
Tempo
Prometheus
Grafana
Loki
```

La regla general es:

```text
local = desarrollo rápido
kubernetes = integración completa
producción = observabilidad completa
```

## Flujo recomendado

### Encendido

```bash
docker compose -f ./platform/docker/docker-compose.dev.yml up -d

./scripts/dev-up.sh

./scripts/run-services.sh

./scripts/health-check.sh
```

### Apagado

```bash
./scripts/dev-stop-services.sh

docker compose -f ./platform/docker/docker-compose.dev.yml down
```
