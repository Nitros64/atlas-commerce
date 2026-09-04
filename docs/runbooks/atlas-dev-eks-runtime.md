# Atlas Commerce DEV Runtime on EKS

Este runbook explica cómo levantar, validar y destruir el entorno **Atlas Commerce DEV** en AWS usando el flujo automatizado del repositorio.

Atlas DEV usa:

- Terraform para infraestructura AWS.
- EKS como cluster Kubernetes.
- RDS PostgreSQL externo.
- ElastiCache Redis externo con TLS.
- AWS Secrets Manager.
- External Secrets Operator.
- Helm para desplegar Atlas Commerce.
- Kafka dentro del cluster con PVC EBS.

> Este entorno es de desarrollo y puede generar costes. Destruirlo al terminar las pruebas.

---

## 1. Requisitos

Herramientas necesarias:

```bash
aws --version
terraform version
kubectl version --client
helm version
openssl version
```

Contexto esperado:

```bash
aws sts get-caller-identity
```

Valores DEV:

```text
Region:    eu-central-1
Cluster:   atlas-commerce-dev
Namespace: atlas
```

---

## 2. Flujo recomendado

El camino principal para operar DEV son los scripts. Los comandos manuales quedan solo para troubleshooting.

### Crear DEV completo

```bash
./scripts/aws/create-dev-now.sh
```

Este script ejecuta el ciclo completo:

1. Limpia planes Terraform viejos.
2. Maneja el secret platform si quedó `scheduled for deletion`.
3. Ejecuta `terraform plan/apply`.
4. Actualiza `kubeconfig`.
5. Instala External Secrets Operator.
6. Siembra el secret platform en AWS Secrets Manager.
7. Escala el nodegroup DEV a 3 nodos.
8. Valida el render de Helm DEV.
9. Despliega Atlas con Helm.
10. Ejecuta el chequeo estructural del runtime (rollouts, pods y configuración).

La prueba HTTP funcional se ejecuta manualmente después, mediante el
port-forward descrito en la sección 10.

### Validar runtime

```bash
./scripts/k8s/check-dev-runtime.sh
```

Valida:

- Namespace `atlas`.
- Nodos Kubernetes.
- ExternalSecrets.
- Job `postgres-bootstrap`.
- Rollout de todos los deployments.
- Estado de pods.
- Services.
- Variables críticas de Redis TLS.
- Variables críticas de Hikari/RDS.

### Destruir DEV

```bash
./scripts/aws/destroy-dev-now.sh
```

El script intenta limpiar:

- Helm release `atlas`.
- External Secrets Operator.
- PVCs/PVs.
- Namespaces.
- Recursos Terraform.
- Secret platform si quedó `scheduled for deletion`.
- Volúmenes EBS huérfanos.
- ENIs huérfanas de Kubernetes.
- Security Groups huérfanos de EKS.

Si `terraform destroy` falla por dependencias de subnet/VPC, el script ejecuta limpieza de huérfanos y reintenta el destroy.

### Validar coste cero

```bash
./scripts/aws/cost-control-check-dev.sh
```

Después de destruir DEV, esta validación debe quedar vacía o sin recursos de Atlas en:

- EKS.
- EC2.
- EBS.
- Elastic IPs.
- NAT Gateways.
- Load Balancers.
- RDS.
- ElastiCache.
- VPCs.
- Security Groups.
- ENIs huérfanas.
- Snapshots.
- Secrets Manager.
- S3 buckets.

---

## 3. Scripts principales

### AWS

```text
scripts/aws/create-dev-now.sh
scripts/aws/destroy-dev-now.sh
scripts/aws/cost-control-check-dev.sh
scripts/aws/seed-dev-platform-secret.sh
```

### Helm

```text
scripts/helm/deploy-dev.sh
scripts/helm/install-external-secrets-dev.sh
scripts/helm/validate-dev.sh
```

### Kubernetes

```text
scripts/k8s/check-dev-runtime.sh
scripts/smoke/dev-smoke.sh
```

---

## 4. Validación de Helm DEV

Para validar Helm sin desplegar:

```bash
./scripts/helm/validate-dev.sh
```

Este script ejecuta:

- `helm lint`.
- `helm template`.
- Validación de Redis TLS.
- Validación de Hikari DEV.
- Validación de Kafka PVC `gp2`.
- Validación de `subPath: kafka`.

Resultado esperado:

```text
DEV Helm validation passed.
```

---

## 5. Estado esperado del runtime

Pods esperados:

```text
audit          1/1 Running
auth           1/1 Running
cart           1/1 Running
catalog        1/1 Running
coupon         1/1 Running
gateway        1/1 Running
inventory      1/1 Running
kafka          1/1 Running
notification   1/1 Running
order          1/1 Running
payment        1/1 Running
pricing        1/1 Running
shipping       1/1 Running
```

Services esperados:

```text
gateway-service
auth-service
catalog-service
cart-service
order-service
payment-service
shipping-service
notification-service
audit-service
kafka
```

PostgreSQL debe aparecer como `ExternalName` apuntando al endpoint de RDS:

```text
postgres-auth
postgres-catalog
postgres-order
...
```

---

## 6. PostgreSQL bootstrap

El job `postgres-bootstrap` crea las bases:

```text
auditdb
authdb
cartdb
catalogdb
coupondb
inventorydb
notificationdb
orderdb
paymentdb
shippingdb
```

Validar:

```bash
kubectl get job postgres-bootstrap -n atlas
kubectl logs -n atlas -l app=postgres-bootstrap --all-containers=true --tail=100
```

Estado esperado:

```text
postgres-bootstrap Complete 1/1
PostgreSQL bootstrap completed
```

---

## 7. Configuración crítica DEV

### Redis TLS

ElastiCache Redis en DEV usa TLS. Las aplicaciones deben tener:

```text
SPRING_DATA_REDIS_SSL_ENABLED=true
```

Validar:

```bash
kubectl exec -n atlas deployment/catalog -- printenv | grep SPRING_DATA_REDIS
kubectl exec -n atlas deployment/gateway -- printenv | grep SPRING_DATA_REDIS
```

### Hikari/RDS

DEV usa una instancia RDS pequeña, así que los pools Hikari deben estar limitados:

```text
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=4
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=0
SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT=60000
```

Validar:

```bash
kubectl exec -n atlas deployment/auth -- printenv | grep SPRING_DATASOURCE_HIKARI
```

### Kafka PVC

En EKS debe usarse StorageClass:

```text
gp2
```

Kafka debe montar el volumen con `subPath` para evitar el error `lost+found`:

```yaml
volumeMounts:
  - name: kafka-data
    mountPath: /var/lib/kafka/data
    subPath: kafka
```

---

## 8. Troubleshooting

### ExternalSecret no existe

Síntoma:

```text
no matches for kind "ExternalSecret"
no matches for kind "ClusterSecretStore"
```

Causa:

```text
External Secrets Operator no está instalado en el cluster nuevo.
```

Solución:

```bash
./scripts/helm/install-external-secrets-dev.sh
```

---

### Secret scheduled for deletion

Síntoma:

```text
You can't create this secret because a secret with this name is already scheduled for deletion
```

Solución para DEV:

```bash
aws secretsmanager restore-secret \
  --region eu-central-1 \
  --secret-id atlas-commerce/dev/platform

aws secretsmanager delete-secret \
  --region eu-central-1 \
  --secret-id atlas-commerce/dev/platform \
  --force-delete-without-recovery
```

Luego generar un plan nuevo:

```bash
rm -f platform/terraform/live/aws/dev/eks.tfplan
terraform -chdir=platform/terraform/live/aws/dev plan -out=eks.tfplan
terraform -chdir=platform/terraform/live/aws/dev apply eks.tfplan
```

---

### Pods Pending por memoria

Síntoma:

```text
Insufficient memory
```

Solución:

```bash
aws eks update-nodegroup-config \
  --region eu-central-1 \
  --cluster-name atlas-commerce-dev \
  --nodegroup-name default \
  --scaling-config minSize=1,maxSize=3,desiredSize=3
```

---

### Kafka PVC Pending

Síntoma:

```text
pod has unbound immediate PersistentVolumeClaims
```

Causa:

```text
StorageClass incorrecta para EKS.
```

Solución:

```bash
./scripts/helm/validate-dev.sh
```

En DEV debe renderizarse:

```text
storageClassName: gp2
```

---

### Kafka lost+found

Síntoma:

```text
Found directory /var/lib/kafka/data/lost+found
Kafka's log directories should only contain Kafka topic data
```

Solución:

```text
Usar subPath: kafka en el volumeMount de Kafka.
```

---

### Redis timeout

Síntoma:

```text
Unable to connect to Redis
Connection initialization timed out
```

Causa:

```text
ElastiCache Redis tiene TLS habilitado.
```

Solución:

```text
SPRING_DATA_REDIS_SSL_ENABLED=true
```

---

### RDS remaining connection slots

Síntoma:

```text
FATAL: remaining connection slots are reserved for roles with privileges of the "pg_use_reserved_connections" role
```

Causa:

```text
Demasiados microservicios abriendo pools Hikari grandes contra una instancia RDS pequeña.
```

Solución DEV actual:

```text
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=4
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=0
SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT=60000
```

---

### HikariPool timeout con pool 2

Síntoma:

```text
HikariPool-1 - Connection is not available, request timed out
total=2, active=2, idle=0
```

Causa:

```text
Pool 2 es demasiado bajo para el arranque con Flyway + JPA.
```

Solución:

```text
Usar maximumPoolSize=4 en DEV.
```

---

### ElastiCache stuck in creating

Síntoma:

```text
atlas-commerce-dev-redis creating
Endpoint None
Nodes None
```

Revisar:

```bash
aws elasticache describe-replication-groups \
  --region eu-central-1 \
  --replication-group-id atlas-commerce-dev-redis \
  --query "ReplicationGroups[0].{Status:Status,Endpoint:NodeGroups[0].PrimaryEndpoint.Address,Members:MemberClusters}" \
  --output table
```

Si pasa a `available`, se puede borrar:

```bash
aws elasticache delete-replication-group \
  --region eu-central-1 \
  --replication-group-id atlas-commerce-dev-redis \
  --no-retain-primary-cluster
```

---

### DependencyViolation al borrar subnet

Síntoma:

```text
DependencyViolation: The subnet has dependencies and cannot be deleted.
```

Causa común:

```text
ENI huérfana de Kubernetes/EKS.
```

Detectar:

```bash
./scripts/aws/cost-control-check-dev.sh
```

Si la ENI está `available`, `Instance=None` y `RequesterManaged=False`, puede borrarse:

```bash
aws ec2 delete-network-interface \
  --region eu-central-1 \
  --network-interface-id <eni-id>
```

`destroy-dev-now.sh` intenta limpiar este caso automáticamente y reintentar `terraform destroy`.

---

### DependencyViolation al borrar VPC

Síntoma:

```text
DependencyViolation: The vpc has dependencies and cannot be deleted.
```

Causa común:

```text
Security Group huérfano de EKS.
```

Detectar:

```bash
./scripts/aws/cost-control-check-dev.sh
```

Si solo queda el security group de EKS y el cluster ya no existe:

```bash
aws ec2 delete-security-group \
  --region eu-central-1 \
  --group-id <sg-id>
```

El security group `default` de la VPC no se borra manualmente; desaparece cuando la VPC se elimina.

---

## 9. Comandos manuales de referencia

El flujo recomendado es usar los scripts. Estos comandos quedan solo como respaldo.

### Terraform manual

```bash
rm -f platform/terraform/live/aws/dev/eks.tfplan
terraform -chdir=platform/terraform/live/aws/dev plan -out=eks.tfplan
terraform -chdir=platform/terraform/live/aws/dev apply eks.tfplan
```

### Kubeconfig manual

```bash
aws eks update-kubeconfig \
  --region eu-central-1 \
  --name atlas-commerce-dev
```

### Deploy Helm manual

```bash
./scripts/helm/deploy-dev.sh
```

### Destroy manual

```bash
terraform -chdir=platform/terraform/live/aws/dev destroy -auto-approve
```

Preferir siempre:

```bash
./scripts/aws/destroy-dev-now.sh
```

---

## 10. Acceso funcional y smoke HTTP

DEV no renderiza `atlas-ingress`: el repositorio no instala un controlador
nginx y tampoco crea un `LoadBalancer`. El gateway permanece privado como
`ClusterIP`. Desde una estación de operador con kubeconfig de DEV, abrir un
port-forward en una terminal:

```bash
kubectl -n atlas port-forward service/gateway-service 18080:80
```

En otra terminal, ejecutar:

```bash
bash scripts/smoke/dev-smoke.sh
```

El script usa `http://127.0.0.1:18080` por defecto y comprueba, con reintentos
acotados:

1. que el readiness del gateway responde HTTP 200 con estado `UP`;
2. que el gateway enruta `GET /api/v1/auth/ping` y recibe `auth-service up`.

La segunda comprobación valida tráfico HTTP real entre gateway y auth; no se
limita a mirar pods `Ready`. No crea usuarios ni otros datos. Ante cualquier
fallo muestra URL, estado y respuesta, y termina con código distinto de cero.

Puede apuntarse a otra ruta sin modificar el script:

```bash
BASE_URL=http://gateway-service bash scripts/smoke/dev-smoke.sh
```

Este contrato permite envolver el script en un Job `PostSync` de Argo CD más
adelante. Esta entrega no crea ese hook.

### Rollback manual de una promoción

La imagen deseada vive en Git. Para revertir una promoción, revertir el commit
que cambió `values/images.dev.yaml`, publicar el revert mediante PR y dejar que
Argo CD vuelva a reconciliar ese estado:

```bash
git revert <commit-de-la-promocion>
git push origin <rama-de-rollback>
```

Mientras DEV conserve sincronización manual, un operador debe sincronizar la
aplicación tras fusionar el revert. No hay rollback automático en esta entrega.

### Significado de local-lite

`values/local-lite.yaml` no elimina componentes. Mantiene Services, ConfigMaps
y Secrets, pero escala a cero los Deployments de payment, shipping y audit.

### Rollout con una réplica

Los doce Deployments de aplicación usan `maxUnavailable: 0` y `maxSurge: 1`:
el pod anterior sigue disponible hasta que el nuevo esté listo. Esto requiere
capacidad temporal para un pod adicional durante cada actualización. Kafka no
usa esa estrategia porque su PVC singleton necesita un tratamiento separado.

El PDB de auth queda disponible mediante `auth.pdb.enabled`, pero desactivado
por defecto: `minAvailable: 1` con una sola réplica bloquearía disrupciones
voluntarias como el drenado de un nodo.
