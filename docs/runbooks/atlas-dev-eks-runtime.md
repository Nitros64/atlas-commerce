# Atlas Commerce DEV Runtime on EKS

Este runbook explica cómo levantar y destruir el entorno **Atlas Commerce DEV** en AWS usando:

* Terraform para infraestructura AWS.
* EKS como cluster Kubernetes.
* RDS PostgreSQL externo.
* ElastiCache Redis externo con TLS.
* AWS Secrets Manager.
* External Secrets Operator.
* Helm para desplegar Atlas Commerce.
* Kafka dentro del cluster con PVC EBS.

> Este entorno es de desarrollo y puede generar costes. Destruirlo al terminar las pruebas.

---

## 1. Requisitos

Herramientas necesarias:

```bash
aws --version
terraform version
kubectl version --client
helm version
```

También se requiere:

```bash
openssl version
```

Contexto esperado:

```bash
aws sts get-caller-identity
```

Región usada:

```text
eu-central-1
```

Cluster esperado:

```text
atlas-commerce-dev
```

Namespace de runtime:

```text
atlas
```

---

## 2. Crear infraestructura AWS con Terraform

Desde la raíz del repositorio:

```bash
terraform -chdir=platform/terraform/live/aws/dev plan -out=eks.tfplan
terraform -chdir=platform/terraform/live/aws/dev apply eks.tfplan
```

Al finalizar, Terraform debe mostrar outputs similares a:

```text
rds_postgresql_address
rds_postgresql_master_secret_arn
redis_primary_endpoint
platform_secret_name
velero_backup_bucket_name
```

---

## 3. Actualizar kubeconfig

Después de crear o recrear EKS:

```bash
aws eks update-kubeconfig \
  --region eu-central-1 \
  --name atlas-commerce-dev
```

Validar:

```bash
kubectl get nodes -o wide
kubectl get pods -n kube-system
```

El nodo debe estar en estado:

```text
Ready
```

---

## 4. Instalar External Secrets Operator

Cada vez que se destruye y recrea EKS, hay que reinstalar External Secrets Operator porque los CRDs viven dentro del cluster.

```bash
ExternalSecretsRoleArn=$(aws iam get-role \
  --role-name atlas-commerce-dev-external-secrets-role \
  --query 'Role.Arn' \
  --output text)

helm repo add external-secrets https://charts.external-secrets.io
helm repo update

helm upgrade --install external-secrets external-secrets/external-secrets \
  -n external-secrets \
  --create-namespace \
  --set installCRDs=true \
  --set serviceAccount.create=true \
  --set serviceAccount.name=external-secrets \
  --set-string "serviceAccount.annotations.eks\.amazonaws\.com/role-arn=$ExternalSecretsRoleArn"
```

Validar:

```bash
kubectl get pods -n external-secrets
kubectl get crd | grep external-secrets
```

---

## 5. Sembrar el secret platform en AWS Secrets Manager

Ejecutar:

```bash
./scripts/aws/seed-dev-platform-secret.sh
```

Validar keys:

```bash
aws secretsmanager get-secret-value \
  --region eu-central-1 \
  --secret-id atlas-commerce/dev/platform \
  --query SecretString \
  --output text | python -c "import sys,json; print('\n'.join(json.loads(sys.stdin.read()).keys()))"
```

Debe mostrar:

```text
SECURITY_JWT_SECRET
REDIS_PASSWORD
POSTGRES_PASSWORD
```

---

## 6. Desplegar Atlas con Helm

Ejecutar:

```bash
./scripts/helm/deploy-dev.sh
```

Este script toma outputs de Terraform y los inyecta en Helm:

* Endpoint de RDS.
* Endpoint de Redis.
* Secret platform.
* Secret admin de RDS.
* Redis TLS para dev.
* Configuración externa de PostgreSQL y Redis.

---

## 7. Escalar node group para levantar todo Atlas

El entorno completo de Atlas requiere más de un nodo `t3.medium`.

Para pruebas completas, escalar temporalmente a 3 nodos:

```bash
aws eks update-nodegroup-config \
  --region eu-central-1 \
  --cluster-name atlas-commerce-dev \
  --nodegroup-name default \
  --scaling-config minSize=1,maxSize=3,desiredSize=3
```

Validar:

```bash
kubectl get nodes -o wide
```

Esperar hasta ver 3 nodos en estado:

```text
Ready
```

---

## 8. Validar External Secrets

```bash
kubectl get clustersecretstore
kubectl get externalsecret -n atlas
kubectl get secret -n atlas | grep -E 'jwt-secret|redis-secret|postgres-.*-secret|postgres-admin-secret'
```

Estado esperado:

```text
ClusterSecretStore aws-secrets-manager Ready=True
ExternalSecrets SecretSynced=True
```

---

## 9. Validar PostgreSQL bootstrap

```bash
kubectl get job postgres-bootstrap -n atlas
kubectl logs -n atlas -l app=postgres-bootstrap --all-containers=true --tail=100
```

Estado esperado:

```text
postgres-bootstrap Complete 1/1
PostgreSQL bootstrap completed
```

El bootstrap crea las bases:

```text
auditdb
authdb
cartdb
catalogdb
coupondb
inventorydb
notificationdb
orderdb
shippingdb
```

---

## 10. Validar pods de Atlas
```

Estado esperado:
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

---

## 11. Validar services

```bash
kubectl get svc -n atlas
```

Debe existir:

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

## 12. Probar Redis TLS desde Kubernetes

Crear un pod temporal:

```bash
kubectl run redis-test -n atlas --rm -it \
  --image=redis:7-alpine \
  --restart=Never \
  -- sh
```

Dentro del pod:

```sh
redis-cli --tls \
  -h <redis_primary_endpoint> \
  -p 6379 \
  PING
```

Resultado esperado:

```text
PONG
```

Salir:

```sh
exit
```

---

## 13. Problemas conocidos y solución

### Error: ExternalSecret no existe

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
helm upgrade --install external-secrets external-secrets/external-secrets \
  -n external-secrets \
  --create-namespace \
  --set installCRDs=true
```

---

### Error: secret scheduled for deletion

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

Luego repetir:

```bash
terraform -chdir=platform/terraform/live/aws/dev plan -out=eks.tfplan
terraform -chdir=platform/terraform/live/aws/dev apply eks.tfplan
```

---

### Error: pods Pending por memoria

Síntoma:

```text
Insufficient memory
```

Solución temporal:

```bash
aws eks update-nodegroup-config \
  --region eu-central-1 \
  --cluster-name atlas-commerce-dev \
  --nodegroup-name default \
  --scaling-config minSize=1,maxSize=3,desiredSize=3
```

---

### Error: Kafka PVC Pending

Síntoma:

```text
pod has unbound immediate PersistentVolumeClaims
```

Causa:

```text
StorageClass incorrecta para EKS.
```

En DEV debe usarse:

```text
gp2
```

---

### Error: Kafka lost+found

Síntoma:

```text
Found directory /var/lib/kafka/data/lost+found
Kafka's log directories should only contain Kafka topic data
```

Solución:

```yaml
volumeMounts:
  - name: kafka-data
    mountPath: /var/lib/kafka/data
    subPath: kafka
```

---

### Error: Redis timeout

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

### Error: RDS remaining connection slots

Síntoma:

```text
FATAL: remaining connection slots are reserved for roles with privileges of the "pg_use_reserved_connections" role
```

Causa:

```text
Demasiados microservicios abriendo pools Hikari grandes contra una instancia RDS pequeña.
```

Solución para DEV:

```text
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=2
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=0
SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT=30000
```

---

## 14. Destruir entorno DEV para ahorrar costes

Usar el script:

```bash
./scripts/aws/destroy-dev-now.sh
```

Este script intenta:

1. Desinstalar Helm release `atlas`.
2. Desinstalar `external-secrets`.
3. Borrar namespaces del cluster.
4. Ejecutar `terraform destroy`.
5. Forzar borrado del secret platform si quedó scheduled for deletion.

Validar que EKS desapareció:

```bash
aws eks list-clusters --region eu-central-1
```

No debe aparecer:

```text
atlas-commerce-dev
```

---

## 15. Cost-control checklist

Antes de cerrar la sesión, validar:

```bash
aws eks list-clusters --region eu-central-1
```

```bash
aws rds describe-db-instances \
  --region eu-central-1 \
  --query "DBInstances[].DBInstanceIdentifier"
```

```bash
aws elasticache describe-replication-groups \
  --region eu-central-1 \
  --query "ReplicationGroups[].ReplicationGroupId"
```

Si el entorno DEV fue destruido correctamente, no deberían aparecer:

```text
atlas-commerce-dev
atlas-commerce-dev-postgresql
atlas-commerce-dev-redis
```

---

## 16. Flujo resumido

Crear DEV:

```bash
terraform -chdir=platform/terraform/live/aws/dev plan -out=eks.tfplan
terraform -chdir=platform/terraform/live/aws/dev apply eks.tfplan

aws eks update-kubeconfig \
  --region eu-central-1 \
  --name atlas-commerce-dev

ExternalSecretsRoleArn=$(aws iam get-role \
  --role-name atlas-commerce-dev-external-secrets-role \
  --query 'Role.Arn' \
  --output text)

helm upgrade --install external-secrets external-secrets/external-secrets \
  -n external-secrets \
  --create-namespace \
  --set installCRDs=true \
  --set serviceAccount.create=true \
  --set serviceAccount.name=external-secrets \
  --set-string "serviceAccount.annotations.eks\.amazonaws\.com/role-arn=$ExternalSecretsRoleArn"

./scripts/aws/seed-dev-platform-secret.sh

aws eks update-nodegroup-config \
  --region eu-central-1 \
  --cluster-name atlas-commerce-dev \
  --nodegroup-name default \
  --scaling-config minSize=1,maxSize=3,desiredSize=3

kubectl get nodes -o wide

./scripts/helm/deploy-dev.sh

kubectl get pods -n atlas -o wide
```

Destruir DEV:

```bash
./scripts/aws/destroy-dev-now.sh
```

```text
audit          1/1 Running

```bash
kubectl get pods -n atlas -o wide

