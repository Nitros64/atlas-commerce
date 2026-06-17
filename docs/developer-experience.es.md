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
chmod +x ./scripts/dev/dev-up.sh
./scripts/dev/dev-up.sh
```

## Ejecutar los microservicios

### Windows

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-services.ps1
```

### Linux

```bash
chmod +x ./scripts/dev/run-services.sh
./scripts/dev/run-services.sh
```

## Verificar el estado de la plataforma

### Windows

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\health-check.ps1
```

### Linux

```bash
chmod +x ./scripts/dev/health-check.sh
./scripts/dev/health-check.sh
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
chmod +x ./scripts/dev/dev-stop-services.sh
./scripts/dev/dev-stop-services.sh
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

./scripts/dev/dev-up.sh

./scripts/dev/run-services.sh

./scripts/dev/health-check.sh
```

### Apagado

```bash
./scripts/dev/dev-stop-services.sh

docker compose -f ./platform/docker/docker-compose.dev.yml down
```
## Despliegue local en Kubernetes con Helm

Atlas Commerce incluye un Helm Chart modular para desplegar localmente los servicios principales de la plataforma sobre Minikube.

### Componentes actuales del despliegue Helm

El despliegue local con Helm incluye actualmente:

```text
auth-service
gateway-service
catalog-service
redis
kafka
postgres-auth
postgres-catalog
```

El Helm Chart se encuentra en:

```text
platform/helm/atlas-commerce
```

El namespace local usado por defecto es:

```text
atlas-helm
```

### Estructura de values

El chart usa archivos `values` separados por componente:

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

Importante:

```text
secrets-local.yaml no debe subirse al repositorio.
```

Este archivo contiene secretos locales como el JWT secret, passwords de base de datos y passwords de servicios internos.

### Scripts auxiliares de Helm

Para evitar ejecutar comandos largos de Helm manualmente, el proyecto incluye scripts auxiliares en:

```text
scripts/helm/
```

Scripts disponibles:

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

### Uso en Linux / Git Bash

Renderizar los manifiestos de Helm:

```bash
./scripts/helm/template-local.sh
```

Ejecutar un dry-run de Helm:

```bash
./scripts/helm/dry-run-local.sh
```

Instalar o actualizar el release local:

```bash
./scripts/helm/upgrade-local.sh
```

Consultar estado de Helm, pods, services y PVCs:

```bash
./scripts/helm/status-local.sh
```

### Uso en Windows PowerShell

PowerShell puede bloquear la ejecución de archivos `.ps1` por defecto. Para permitir scripts solo en la terminal actual, ejecutar:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

Luego ejecutar:

```powershell
.\scripts\helm\template-local.ps1
.\scripts\helm\dry-run-local.ps1
.\scripts\helm\upgrade-local.ps1
.\scripts\helm\status-local.ps1
```

También se puede ejecutar un script directamente con bypass:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\helm\template-local.ps1
```

### Validar el despliegue local

Verificar pods:

```bash
kubectl get pods -n atlas-helm
```

Verificar services:

```bash
kubectl get svc -n atlas-helm
```

Verificar volúmenes persistentes:

```bash
kubectl get pvc -n atlas-helm
```

Verificar estado del release de Helm:

```bash
helm status atlas -n atlas-helm
```

### Exponer el API Gateway localmente

Abrir un port-forward hacia el Gateway:

```bash
kubectl port-forward -n atlas-helm svc/gateway-service 8080:80
```

Luego probar:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/auth/v3/api-docs
curl http://localhost:8080/catalog/v3/api-docs
```

Resultado esperado:

```text
Gateway health: UP
Auth OpenAPI accesible a través del Gateway
Catalog OpenAPI accesible a través del Gateway
```

### Logs

Consultar logs principales:

```bash
kubectl logs -n atlas-helm deploy/gateway --tail=50
kubectl logs -n atlas-helm deploy/auth --tail=50
kubectl logs -n atlas-helm deploy/catalog --tail=50
kubectl logs -n atlas-helm deploy/kafka --tail=50
```

Verificar que el Gateway local no esté intentando exportar trazas a Tempo/OTLP:

```bash
kubectl logs -n atlas-helm deploy/gateway --since=2m | grep -iE "tempo|otlp|failed to export"
```

Si el comando no imprime nada, significa que la exportación local hacia Tempo/OTLP está correctamente desactivada.
