# Atlas Commerce — Plan para despliegue self-hosted en cualquier cuenta AWS

Este documento describe los pasos para convertir el despliegue actual de **Atlas Commerce DEV en AWS** en un flujo portable, de forma que cualquier persona pueda clonar el repositorio, configurar sus variables y desplegar Atlas en su propia cuenta AWS.

La meta final es que el usuario externo pueda ejecutar algo parecido a esto:

```bash
git clone <repo-atlas-commerce>
cd atlas-commerce

cp .env.aws.example .env.aws
# editar .env.aws con su AWS profile, región, nombre del entorno y preferencias

./scripts/aws/bootstrap-terraform-backend.sh
./scripts/aws/create-dev-now.sh
./scripts/k8s/check-dev-runtime.sh
```

Y al terminar:

```bash
./scripts/aws/destroy-dev-now.sh
./scripts/aws/cost-control-check-dev.sh
```

---

## 1. Objetivo técnico

Hacer que Atlas Commerce no dependa de una cuenta AWS específica, nombres personales, buckets existentes, secretos previos o configuración local del autor original.

El despliegue debe ser configurable mediante:

- Variables de entorno.
- Archivos `.env` ignorados por Git.
- Archivos `.example` versionados.
- Terraform variables.
- Scripts reutilizables.

El resultado debe permitir crear en otra cuenta AWS:

- VPC.
- Subnets públicas y privadas.
- Internet Gateway.
- NAT Gateway, si aplica.
- EKS.
- Node group.
- RDS PostgreSQL.
- ElastiCache Redis con TLS.
- AWS Secrets Manager.
- External Secrets Operator.
- IRSA roles.
- S3 bucket para Terraform state.
- DynamoDB table para Terraform lock.
- Helm deployment de Atlas.
- Limpieza y verificación de coste cero.

---

## 2. Principio importante

El repositorio debe contener ejemplos seguros, pero no configuración real de una cuenta concreta.

Debe estar en Git:

```text
.env.aws.example
terraform.tfvars.example
backend.hcl.example
```

No debe estar en Git:

```text
.env.aws
terraform.tfvars
backend.hcl
*.tfplan
*.tfstate
*.tfstate.backup
```

---

## 3. Crear rama de trabajo

Desde `master`:

```bash
git switch master
git pull
git switch -c feature/self-hosted-aws-deploy
```

---

## 4. Bloque 1 — Crear configuración externa para AWS

### 4.1 Crear `.env.aws.example`

Crear en la raíz del repositorio:

```bash
touch .env.aws.example
```

Contenido recomendado:

```bash
# AWS account/profile configuration
ATLAS_AWS_PROFILE=default
ATLAS_AWS_REGION=eu-central-1

# Atlas environment identity
ATLAS_PROJECT_NAME=atlas-commerce
ATLAS_ENVIRONMENT=dev
ATLAS_OWNER=your-name

# Kubernetes/EKS
ATLAS_CLUSTER_NAME=atlas-commerce-dev
ATLAS_NODEGROUP_NAME=default
ATLAS_NAMESPACE=atlas
ATLAS_DESIRED_NODES=3

# Terraform
ATLAS_TERRAFORM_DIR=platform/terraform/live/aws/dev
ATLAS_TERRAFORM_PLAN_FILE=eks.tfplan
ATLAS_TERRAFORM_BACKEND_CONFIG=backend.hcl

# AWS Secrets Manager
ATLAS_PLATFORM_SECRET_NAME=atlas-commerce/dev/platform

# External Secrets
ATLAS_EXTERNAL_SECRETS_NAMESPACE=external-secrets
ATLAS_EXTERNAL_SECRETS_SERVICE_ACCOUNT=external-secrets
ATLAS_EXTERNAL_SECRETS_ROLE_NAME=atlas-commerce-dev-external-secrets-role

# Images
ATLAS_IMAGE_REGISTRY=docker.io/nitros64
ATLAS_IMAGE_TAG=latest

# Cost-control
ATLAS_ENABLE_COST_GUARDS=true
```

### 4.2 Crear `.gitignore` o actualizarlo

Validar que `.gitignore` tenga:

```gitignore
.env.aws
*.tfplan
*.tfstate
*.tfstate.backup
backend.hcl
terraform.tfvars
.terraform/
```

Si no existe, agregarlo.

### 4.3 Crear loader común

Crear directorio:

```bash
mkdir -p scripts/lib
```

Crear archivo:

```bash
touch scripts/lib/load-atlas-env.sh
chmod +x scripts/lib/load-atlas-env.sh
```

Contenido recomendado:

```bash
#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="$PROJECT_ROOT/.env.aws"

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
else
  echo "WARNING: .env.aws not found. Using built-in defaults."
  echo "Create one with: cp .env.aws.example .env.aws"
fi

export ATLAS_AWS_PROFILE="${ATLAS_AWS_PROFILE:-default}"
export ATLAS_AWS_REGION="${ATLAS_AWS_REGION:-eu-central-1}"
export ATLAS_PROJECT_NAME="${ATLAS_PROJECT_NAME:-atlas-commerce}"
export ATLAS_ENVIRONMENT="${ATLAS_ENVIRONMENT:-dev}"
export ATLAS_OWNER="${ATLAS_OWNER:-unknown}"

export ATLAS_CLUSTER_NAME="${ATLAS_CLUSTER_NAME:-atlas-commerce-dev}"
export ATLAS_NODEGROUP_NAME="${ATLAS_NODEGROUP_NAME:-default}"
export ATLAS_NAMESPACE="${ATLAS_NAMESPACE:-atlas}"
export ATLAS_DESIRED_NODES="${ATLAS_DESIRED_NODES:-3}"

export ATLAS_TERRAFORM_DIR="${ATLAS_TERRAFORM_DIR:-platform/terraform/live/aws/dev}"
export ATLAS_TERRAFORM_PLAN_FILE="${ATLAS_TERRAFORM_PLAN_FILE:-eks.tfplan}"
export ATLAS_TERRAFORM_BACKEND_CONFIG="${ATLAS_TERRAFORM_BACKEND_CONFIG:-backend.hcl}"

export ATLAS_PLATFORM_SECRET_NAME="${ATLAS_PLATFORM_SECRET_NAME:-atlas-commerce/dev/platform}"

export ATLAS_EXTERNAL_SECRETS_NAMESPACE="${ATLAS_EXTERNAL_SECRETS_NAMESPACE:-external-secrets}"
export ATLAS_EXTERNAL_SECRETS_SERVICE_ACCOUNT="${ATLAS_EXTERNAL_SECRETS_SERVICE_ACCOUNT:-external-secrets}"
export ATLAS_EXTERNAL_SECRETS_ROLE_NAME="${ATLAS_EXTERNAL_SECRETS_ROLE_NAME:-atlas-commerce-dev-external-secrets-role}"

export ATLAS_IMAGE_REGISTRY="${ATLAS_IMAGE_REGISTRY:-docker.io/nitros64}"
export ATLAS_IMAGE_TAG="${ATLAS_IMAGE_TAG:-latest}"
export ATLAS_ENABLE_COST_GUARDS="${ATLAS_ENABLE_COST_GUARDS:-true}"

aws_args() {
  if [[ -n "${ATLAS_AWS_PROFILE:-}" && "${ATLAS_AWS_PROFILE}" != "default" ]]; then
    echo "--profile ${ATLAS_AWS_PROFILE} --region ${ATLAS_AWS_REGION}"
  else
    echo "--region ${ATLAS_AWS_REGION}"
  fi
}
```

### 4.4 Validar sintaxis

```bash
bash -n scripts/lib/load-atlas-env.sh
```

### 4.5 Commit sugerido

```bash
git add .env.aws.example .gitignore scripts/lib/load-atlas-env.sh

git commit -m "chore(config): add portable AWS environment configuration" \
  -m "Add .env.aws.example and a shared script loader for Atlas AWS deployment settings."
```

---

## 5. Bloque 2 — Refactorizar scripts para usar variables externas

Actualizar estos scripts para que carguen `scripts/lib/load-atlas-env.sh`:

```text
scripts/aws/create-dev-now.sh
scripts/aws/destroy-dev-now.sh
scripts/aws/cost-control-check-dev.sh
scripts/aws/seed-dev-platform-secret.sh
scripts/helm/install-external-secrets-dev.sh
scripts/helm/deploy-dev.sh
scripts/k8s/check-dev-runtime.sh
```

Al inicio de cada script agregar:

```bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
# shellcheck disable=SC1091
source "$PROJECT_ROOT/scripts/lib/load-atlas-env.sh"
```

Luego reemplazar valores hardcodeados.

### 5.1 Reemplazos principales

Cambiar:

```bash
REGION="eu-central-1"
CLUSTER_NAME="atlas-commerce-dev"
NODEGROUP_NAME="default"
NAMESPACE="atlas"
DESIRED_NODES="3"
```

Por:

```bash
REGION="$ATLAS_AWS_REGION"
CLUSTER_NAME="$ATLAS_CLUSTER_NAME"
NODEGROUP_NAME="$ATLAS_NODEGROUP_NAME"
NAMESPACE="$ATLAS_NAMESPACE"
DESIRED_NODES="$ATLAS_DESIRED_NODES"
```

Cambiar:

```bash
platform/terraform/live/aws/dev
```

Por:

```bash
$ATLAS_TERRAFORM_DIR
```

Cambiar:

```bash
atlas-commerce/dev/platform
```

Por:

```bash
$ATLAS_PLATFORM_SECRET_NAME
```

### 5.2 AWS profile

Todos los comandos AWS deben usar el profile configurado.

Opción sencilla:

```bash
export AWS_PROFILE="$ATLAS_AWS_PROFILE"
export AWS_REGION="$ATLAS_AWS_REGION"
export AWS_DEFAULT_REGION="$ATLAS_AWS_REGION"
```

Esto evita repetir `--profile` en todos los comandos.

### 5.3 Validar scripts

```bash
bash -n scripts/aws/create-dev-now.sh
bash -n scripts/aws/destroy-dev-now.sh
bash -n scripts/aws/cost-control-check-dev.sh
bash -n scripts/aws/seed-dev-platform-secret.sh
bash -n scripts/helm/install-external-secrets-dev.sh
bash -n scripts/helm/deploy-dev.sh
bash -n scripts/k8s/check-dev-runtime.sh
```

### 5.4 Buscar hardcodes pendientes

```bash
grep -R "eu-central-1\|atlas-commerce-dev\|atlas-commerce/dev/platform\|529601496188" \
  scripts platform/terraform platform/helm docs \
  -n
```

No todo hardcode es necesariamente malo, pero cualquier aparición debe revisarse.

### 5.5 Commit sugerido

```bash
git add scripts

git commit -m "refactor(scripts): load AWS deployment settings from environment" \
  -m "Make Atlas lifecycle scripts configurable through .env.aws and shared defaults."
```

---

## 6. Bloque 3 — Bootstrap del backend remoto de Terraform

Terraform necesita guardar estado en la cuenta AWS del usuario externo. No puede depender de un bucket del autor original.

Crear script:

```bash
touch scripts/aws/bootstrap-terraform-backend.sh
chmod +x scripts/aws/bootstrap-terraform-backend.sh
```

Responsabilidad del script:

1. Leer `.env.aws`.
2. Obtener AWS Account ID.
3. Crear bucket S3 único para Terraform state.
4. Activar versioning.
5. Activar encryption.
6. Crear DynamoDB table para locks.
7. Generar `backend.hcl` local.

Ejemplo de nombres:

```text
atlas-commerce-<account-id>-<region>-tfstate
atlas-commerce-terraform-locks
```

Contenido base recomendado:

```bash
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
# shellcheck disable=SC1091
source "$PROJECT_ROOT/scripts/lib/load-atlas-env.sh"

export AWS_PROFILE="$ATLAS_AWS_PROFILE"
export AWS_REGION="$ATLAS_AWS_REGION"
export AWS_DEFAULT_REGION="$ATLAS_AWS_REGION"

ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
STATE_BUCKET="${ATLAS_PROJECT_NAME}-${ACCOUNT_ID}-${ATLAS_AWS_REGION}-tfstate"
LOCK_TABLE="${ATLAS_PROJECT_NAME}-terraform-locks"
BACKEND_FILE="$PROJECT_ROOT/$ATLAS_TERRAFORM_DIR/backend.hcl"

aws s3api create-bucket \
  --bucket "$STATE_BUCKET" \
  --region "$ATLAS_AWS_REGION" \
  --create-bucket-configuration LocationConstraint="$ATLAS_AWS_REGION" \
  >/dev/null 2>&1 || true

aws s3api put-bucket-versioning \
  --bucket "$STATE_BUCKET" \
  --versioning-configuration Status=Enabled

aws s3api put-bucket-encryption \
  --bucket "$STATE_BUCKET" \
  --server-side-encryption-configuration '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'

aws dynamodb create-table \
  --table-name "$LOCK_TABLE" \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  >/dev/null 2>&1 || true

aws dynamodb wait table-exists --table-name "$LOCK_TABLE"

cat > "$BACKEND_FILE" <<EOF_BACKEND
bucket         = "$STATE_BUCKET"
key            = "${ATLAS_ENVIRONMENT}/terraform.tfstate"
region         = "$ATLAS_AWS_REGION"
dynamodb_table = "$LOCK_TABLE"
encrypt        = true
EOF_BACKEND

echo "Terraform backend created."
echo "Backend file: $BACKEND_FILE"
```

Validar:

```bash
bash -n scripts/aws/bootstrap-terraform-backend.sh
```

Commit sugerido:

```bash
git add scripts/aws/bootstrap-terraform-backend.sh

git commit -m "feat(terraform): add AWS backend bootstrap script" \
  -m "Create S3 state bucket, DynamoDB lock table and local backend configuration for self-hosted AWS deployments."
```

---

## 7. Bloque 4 — Parametrizar Terraform para cuentas externas

### 7.1 Crear examples seguros

En:

```text
platform/terraform/live/aws/dev/
```

Crear:

```text
terraform.tfvars.example
backend.hcl.example
```

Ejemplo de `terraform.tfvars.example`:

```hcl
project_name = "atlas-commerce"
environment  = "dev"
aws_region   = "eu-central-1"

vpc_cidr = "10.20.0.0/16"

availability_zones = [
  "eu-central-1a",
  "eu-central-1b"
]

tags = {
  Project     = "atlas-commerce"
  Environment = "dev"
  Owner       = "your-name"
}

recovery_window_in_days = 0
```

Ejemplo de `backend.hcl.example`:

```hcl
bucket         = "atlas-commerce-ACCOUNT_ID-eu-central-1-tfstate"
key            = "dev/terraform.tfstate"
region         = "eu-central-1"
dynamodb_table = "atlas-commerce-terraform-locks"
encrypt        = true
```

### 7.2 Inicialización Terraform portable

El usuario externo debería ejecutar:

```bash
cp platform/terraform/live/aws/dev/terraform.tfvars.example \
   platform/terraform/live/aws/dev/terraform.tfvars

./scripts/aws/bootstrap-terraform-backend.sh

terraform -chdir=platform/terraform/live/aws/dev init \
  -backend-config=backend.hcl
```

### 7.3 Revisar hardcodes en Terraform

Buscar:

```bash
grep -R "529601496188\|nitro\|eu-central-1\|atlas-commerce-dev" \
  platform/terraform \
  -n
```

Cualquier account id real debe eliminarse.

### 7.4 Commit sugerido

```bash
git add platform/terraform/live/aws/dev/terraform.tfvars.example \
        platform/terraform/live/aws/dev/backend.hcl.example

git commit -m "docs(terraform): add self-hosted tfvars and backend examples" \
  -m "Provide safe example files for configuring Terraform in external AWS accounts."
```

---

## 8. Bloque 5 — Ajustar Terraform init dentro de create-dev-now.sh

El script `create-dev-now.sh` debe poder ejecutar `terraform init` usando el backend local generado.

Agregar antes de `terraform plan`:

```bash
BACKEND_CONFIG="$PROJECT_ROOT/$ATLAS_TERRAFORM_DIR/$ATLAS_TERRAFORM_BACKEND_CONFIG"

if [[ -f "$BACKEND_CONFIG" ]]; then
  terraform -chdir="$TERRAFORM_DIR" init -backend-config="$ATLAS_TERRAFORM_BACKEND_CONFIG"
else
  echo "ERROR: Terraform backend config not found: $BACKEND_CONFIG" >&2
  echo "Run: ./scripts/aws/bootstrap-terraform-backend.sh" >&2
  exit 1
fi
```

Esto hace que un usuario nuevo no falle por no tener backend inicializado.

Commit sugerido:

```bash
git add scripts/aws/create-dev-now.sh

git commit -m "feat(terraform): initialize backend during dev creation" \
  -m "Use the generated backend.hcl file before planning and applying the DEV infrastructure."
```

---

## 9. Bloque 6 — Parametrizar imágenes Docker

Hay dos modos posibles.

### Modo A — Usar imágenes públicas por defecto

Este es el modo más fácil para usuarios externos.

Ejemplo:

```text
ATLAS_IMAGE_REGISTRY=docker.io/nitros64
ATLAS_IMAGE_TAG=latest
```

El Helm chart usa esas imágenes sin que el usuario tenga que compilar.

### Modo B — Usuario compila y publica sus propias imágenes

Crear scripts futuros:

```text
scripts/docker/build-all.sh
scripts/docker/push-all.sh
```

Variables:

```bash
ATLAS_IMAGE_REGISTRY=docker.io/your-user
ATLAS_IMAGE_TAG=1.0.0
```

Luego:

```bash
./scripts/docker/build-all.sh
./scripts/docker/push-all.sh
./scripts/aws/create-dev-now.sh
```

### 9.1 Ajustar deploy-dev.sh

`deploy-dev.sh` debe pasar a Helm:

```bash
--set global.imageRegistry="$ATLAS_IMAGE_REGISTRY" \
--set global.imageTag="$ATLAS_IMAGE_TAG"
```

O equivalente según la estructura real del chart.

### 9.2 Commit sugerido

```bash
git add scripts/helm/deploy-dev.sh platform/helm/atlas-commerce

git commit -m "feat(helm): make service images configurable" \
  -m "Allow self-hosted deployments to override image registry and tag through environment configuration."
```

---

## 10. Bloque 7 — Secrets portable

Los secrets deben crearse en la cuenta AWS del usuario externo.

El script actual `seed-dev-platform-secret.sh` debe usar:

```bash
ATLAS_PLATFORM_SECRET_NAME
ATLAS_AWS_REGION
ATLAS_AWS_PROFILE
```

Y no valores hardcodeados.

El secret debe contener:

```text
SECURITY_JWT_SECRET
REDIS_PASSWORD
POSTGRES_PASSWORD
```

El script debe ser idempotente:

- Si el secret no existe, lo crea.
- Si existe, actualiza su valor.
- Si está scheduled for deletion en DEV, lo restaura y fuerza borrado/recreación si hace falta.

Commit sugerido:

```bash
git add scripts/aws/seed-dev-platform-secret.sh

git commit -m "refactor(secrets): make platform secret seeding portable" \
  -m "Use external AWS configuration for Secrets Manager names, region and profile."
```

---

## 11. Bloque 8 — External Secrets portable

`install-external-secrets-dev.sh` debe usar:

```bash
ATLAS_EXTERNAL_SECRETS_NAMESPACE
ATLAS_EXTERNAL_SECRETS_SERVICE_ACCOUNT
ATLAS_EXTERNAL_SECRETS_ROLE_NAME
ATLAS_CLUSTER_NAME
ATLAS_AWS_REGION
ATLAS_AWS_PROFILE
```

Debe resolver el role ARN dinámicamente:

```bash
aws iam get-role \
  --role-name "$ATLAS_EXTERNAL_SECRETS_ROLE_NAME" \
  --query 'Role.Arn' \
  --output text
```

Commit sugerido:

```bash
git add scripts/helm/install-external-secrets-dev.sh

git commit -m "refactor(external-secrets): make operator installation portable" \
  -m "Use configurable namespace, service account, IAM role, cluster and AWS profile settings."
```

---

## 12. Bloque 9 — Cost-control portable

`cost-control-check-dev.sh` debe detectar recursos usando tags y nombres configurables, no solo nombres fijos.

Debe usar:

```bash
ATLAS_PROJECT_NAME
ATLAS_ENVIRONMENT
ATLAS_CLUSTER_NAME
ATLAS_AWS_REGION
ATLAS_AWS_PROFILE
```

Idealmente, todo recurso creado por Terraform debe tener tags:

```text
Project=atlas-commerce
Environment=dev
ManagedBy=terraform
```

El script debe revisar:

- EKS clusters.
- EC2 instances.
- EBS volumes.
- Elastic IPs.
- NAT Gateways.
- Load Balancers.
- RDS instances.
- ElastiCache replication groups.
- VPCs.
- Security Groups.
- ENIs.
- Snapshots.
- Secrets Manager secrets.
- S3 buckets.

Commit sugerido:

```bash
git add scripts/aws/cost-control-check-dev.sh

git commit -m "refactor(aws): make cost-control checks account portable" \
  -m "Use configurable project, environment, cluster, region and profile settings when detecting leftover AWS resources."
```

---

## 13. Bloque 10 — Documentación self-hosted

Crear:

```text
docs/runbooks/self-hosted-aws-deploy.es.md
docs/runbooks/self-hosted-aws-deploy.en.md
```

La documentación debe explicar:

1. Requisitos.
2. Costes esperados.
3. Permisos AWS necesarios.
4. Cómo configurar AWS CLI.
5. Cómo copiar `.env.aws.example`.
6. Cómo crear backend remoto de Terraform.
7. Cómo crear `terraform.tfvars`.
8. Cómo ejecutar `create-dev-now.sh`.
9. Cómo validar runtime.
10. Cómo destruir el entorno.
11. Cómo verificar coste cero.
12. Troubleshooting.

### 13.1 Requisitos mínimos

```bash
aws --version
terraform version
kubectl version --client
helm version
openssl version
```

### 13.2 Flujo para usuario externo

```bash
git clone <repo>
cd atlas-commerce

cp .env.aws.example .env.aws
nano .env.aws

cp platform/terraform/live/aws/dev/terraform.tfvars.example \
   platform/terraform/live/aws/dev/terraform.tfvars
nano platform/terraform/live/aws/dev/terraform.tfvars

./scripts/aws/bootstrap-terraform-backend.sh
./scripts/aws/create-dev-now.sh
./scripts/k8s/check-dev-runtime.sh
```

Destruir:

```bash
./scripts/aws/destroy-dev-now.sh
./scripts/aws/cost-control-check-dev.sh
```

Commit sugerido:

```bash
git add docs/runbooks/self-hosted-aws-deploy.es.md \
        docs/runbooks/self-hosted-aws-deploy.en.md

git commit -m "docs: add self-hosted AWS deployment guide" \
  -m "Document how external users can configure, deploy, validate and destroy Atlas Commerce in their own AWS accounts."
```

---

## 14. Bloque 11 — Validación completa en otra cuenta AWS

Probar con un AWS profile diferente:

```bash
aws configure --profile atlas-test
```

Crear `.env.aws`:

```bash
ATLAS_AWS_PROFILE=atlas-test
ATLAS_AWS_REGION=eu-central-1
ATLAS_PROJECT_NAME=atlas-commerce
ATLAS_ENVIRONMENT=dev
ATLAS_OWNER=test-user
ATLAS_CLUSTER_NAME=atlas-commerce-dev
ATLAS_NODEGROUP_NAME=default
ATLAS_NAMESPACE=atlas
ATLAS_DESIRED_NODES=3
ATLAS_PLATFORM_SECRET_NAME=atlas-commerce/dev/platform
```

Ejecutar:

```bash
./scripts/aws/bootstrap-terraform-backend.sh
./scripts/aws/create-dev-now.sh
./scripts/k8s/check-dev-runtime.sh
```

Luego destruir:

```bash
./scripts/aws/destroy-dev-now.sh
./scripts/aws/cost-control-check-dev.sh
```

El test es exitoso si:

- EKS se crea.
- RDS se crea.
- Redis se crea.
- External Secrets sincroniza secretos.
- Helm despliega Atlas.
- Los pods arrancan.
- Redis TLS funciona.
- PostgreSQL bootstrap completa.
- El destroy elimina todo.
- Cost-control queda limpio.

---

## 15. Permisos AWS mínimos aproximados

Para simplificar la primera versión self-hosted, se puede pedir un usuario/rol con permisos amplios de desarrollo.

Servicios requeridos:

- EC2.
- EKS.
- IAM.
- RDS.
- ElastiCache.
- Secrets Manager.
- S3.
- DynamoDB.
- CloudWatch Logs.
- Elastic Load Balancing.
- ACM, si se usa Ingress TLS.
- Route 53, si se usa DNS.

Para una versión inicial, documentar que se necesita un perfil con permisos suficientes para crear infraestructura de desarrollo.

Más adelante se puede crear una política IAM mínima.

---

## 16. Riesgos y decisiones pendientes

### 16.1 Costes AWS

Este despliegue puede generar costes reales por:

- EKS control plane.
- EC2 nodes.
- NAT Gateway.
- RDS.
- ElastiCache.
- EBS volumes.
- Load Balancers.
- Snapshots.

La documentación debe advertir claramente:

```text
Do not deploy this unless you understand AWS costs.
Always run destroy-dev-now.sh and cost-control-check-dev.sh after testing.
```

### 16.2 Región AWS

La región por defecto puede seguir siendo:

```text
eu-central-1
```

Pero debe poder cambiarse con:

```bash
ATLAS_AWS_REGION
```

### 16.3 Nombre del proyecto

Si muchos usuarios despliegan con el mismo nombre dentro de sus cuentas no hay problema, pero dentro de la misma cuenta puede haber colisiones.

Para evitarlo, permitir:

```bash
ATLAS_PROJECT_NAME=my-atlas
ATLAS_ENVIRONMENT=dev
```

### 16.4 Docker images

Decisión inicial recomendada:

```text
Usar imágenes públicas por defecto.
```

Luego agregar scripts para build/push propio.

### 16.5 Producción

Este flujo debe documentarse como DEV, no producción.

No prometer:

- Alta disponibilidad completa.
- DR production-grade.
- Hardening completo.
- WAF obligatorio.
- Certificados production-ready.

---

## 17. Definition of Done

La fase `self-hosted-aws-deploy` estará completa cuando:

- Existe `.env.aws.example`.
- Existe `scripts/lib/load-atlas-env.sh`.
- Los scripts de AWS/Helm/K8s usan variables externas.
- Existe `bootstrap-terraform-backend.sh`.
- Existe `terraform.tfvars.example`.
- Existe `backend.hcl.example`.
- No hay account IDs personales en scripts/Terraform/docs.
- `create-dev-now.sh` funciona con un AWS profile externo.
- `destroy-dev-now.sh` funciona con un AWS profile externo.
- `cost-control-check-dev.sh` valida recursos de la cuenta externa.
- Existe documentación self-hosted en español e inglés.
- Se ha probado el flujo al menos una vez fuera de la cuenta original o con un perfil AWS separado.

---

## 18. Orden recomendado de commits

```text
1. chore(config): add portable AWS environment configuration
2. refactor(scripts): load AWS deployment settings from environment
3. feat(terraform): add AWS backend bootstrap script
4. docs(terraform): add self-hosted tfvars and backend examples
5. feat(terraform): initialize backend during dev creation
6. feat(helm): make service images configurable
7. refactor(secrets): make platform secret seeding portable
8. refactor(external-secrets): make operator installation portable
9. refactor(aws): make cost-control checks account portable
10. docs: add self-hosted AWS deployment guide
```

---

## 19. Resultado esperado para portfolio

Al terminar esta fase, el proyecto puede describirse así:

```text
Atlas Commerce includes a portable self-hosted AWS deployment flow. Any user with an AWS account can clone the repository, configure environment variables, bootstrap Terraform remote state, deploy the full EKS-based development environment, validate the runtime, destroy all resources and run cost-control checks.
```

En español:

```text
Atlas Commerce incluye un flujo portable de despliegue self-hosted en AWS. Cualquier usuario con una cuenta AWS puede clonar el repositorio, configurar variables, crear el backend remoto de Terraform, desplegar el entorno completo sobre EKS, validar el runtime, destruir los recursos y verificar coste cero.
```
