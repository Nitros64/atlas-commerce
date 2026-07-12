# Bitácora de despliegue — `live/aws/dev`

> **Fecha:** 2026-07-12
> **Cuenta AWS:** 724772086459 (perfil CLI `atlas-commerce`, usuario IAM `terraform-admin`)
> **Región:** eu-central-1 (Frankfurt)

Registro del primer despliegue end-to-end del ambiente `dev`: qué funcionó a la primera, qué falló, y qué cambiar en el código para que la próxima vez (este ambiente u otro nuevo, ej. `staging`) funcione sin intervención manual.

---

## 1. Qué funcionó bien a la primera

- **Bootstrap del backend remoto** (`bootstrap/aws-backend`): aplicó sin errores tras el fix de `backend.tf` (ver §2.1). Bucket S3 + seguridad (versioning, encryption, public access block, bucket policy) + lockfile nativo S3, todo correcto al primer intento.
- **Orden de dependencias del `plan`**: VPC → security groups → EKS control plane / RDS / Redis en paralelo → reglas de ingreso cruzadas (RDS/Redis from EKS) → OIDC provider → roles IRSA → addons. Terraform resolvió el grafo de dependencias correctamente, sin necesidad de `-target` ni ajustes manuales de orden.
- **Tiempos de creación de los recursos "pesados"** (sin contar el problema de red):
  - Redis (`aws_elasticache_replication_group`): 5m51s
  - RDS PostgreSQL (`aws_db_instance`): 6m30s
  - EKS control plane (`aws_eks_cluster`): 7m24s
- **Roles IRSA y policy attachments** (Velero, VPC CNI, EBS CSI, ALB Controller, External Secrets): todos se crearon en 0-1s cada uno, sin fricción, en cuanto el OIDC provider estuvo listo.
- **Variables requeridas sin default** (`eks_kubernetes_version`, `eks_cluster_endpoint_public_access_cidrs`, `eks_node_instance_types`, `rds_engine_version`, `redis_engine_version`): el `plan` falla de forma clara y explícita pidiéndolas, no hay sorpresas a mitad de apply.

---

## 2. Qué falló / qué no funcionó a la primera

### 2.1 🔴 Bug bloqueante — `bootstrap/aws-backend/backend.tf` declaraba un backend S3 parcial

**Síntoma:** `terraform init` en `bootstrap/aws-backend` fallaba con:
```
Error: Error asking for input to configure backend "s3": bucket: EOF
```

**Causa:** `backend.tf` tenía `terraform { backend "s3" {} }`, contradiciendo el propio README del folder, que documenta explícitamente que el bootstrap debe usar **estado local** (no puede depender de un backend que él mismo crea — problema chicken-and-egg).

**Fix aplicado:** se quitó el bloque `backend "s3" {}`, dejando `terraform {}` vacío con comentario explicativo.

**Ya corregido en este despliegue** — no debería repetirse.

### 2.2 🔴 Bloqueante — Node group de EKS colgado indefinidamente por falta de NAT Gateway

**Síntoma:** `module.eks.aws_eks_node_group.default` se quedó en `Still creating...` más de 16 minutos sin completar ni fallar explícitamente (el timeout por defecto de este recurso es más largo, ~20-30 min antes de que Terraform reporte error).

**Causa raíz:** `terraform.tfvars` tenía `enable_nat_gateway = false` (para evitar el costo de NAT en dev). Las subnets **privadas**, donde el node group lanza las instancias EC2, solo tenían la ruta local (`10.20.0.0/16`) — **sin ruta `0.0.0.0/0` hacia ningún NAT Gateway ni VPC Endpoint**. Los nodos EC2 no podían descargar las imágenes de kubelet/CNI ni registrarse contra el cluster, así que el Auto Scaling Group nunca llegó a reportar instancias sanas.

**Diagnóstico:**
```bash
aws eks describe-nodegroup --cluster-name atlas-commerce-dev --nodegroup-name default \
  --query 'nodegroup.resources.autoScalingGroups[0].name'
# → None (el ASG nunca se creó con instancias)

aws ec2 describe-nat-gateways --filter "Name=tag:Project,Values=atlas-commerce"
# → [] (no hay NAT Gateway)

aws ec2 describe-route-tables --filters "Name=tag:Project,Values=atlas-commerce"
# → la route table de subnets privadas solo tiene la ruta local, sin 0.0.0.0/0
```

**Acción tomada:**
1. Se interrumpió el `apply` en curso.
2. Quedó un **lockfile S3 huérfano** (`terraform.tfstate.tflock`) — se liberó con `terraform force-unlock -force <lock-id>` (el lock era legítimo, del propio proceso interrumpido).
3. Quedó un **node group huérfano en AWS** (fuera del state de Terraform, en `CREATING`, sin instancias EC2 reales) — se eliminó manualmente con `aws eks delete-nodegroup` antes de re-aplicar, para evitar un conflicto "ya existe" en el próximo `apply`.
4. Se cambió `enable_nat_gateway = false` → `true` en `terraform.tfvars`.
5. Pendiente: re-`plan` + re-`apply` con NAT habilitado.

**Costo del fix:** NAT Gateway ≈ $0.045/hora (~$32/mes) + procesamiento de datos. Aceptado como necesario — no es opcional si el node group vive en subnets privadas.

**Resultado tras el fix:** con NAT Gateway habilitado, el node group completó en **2m44s** (vs. >16 min colgado sin red) — confirma que el diagnóstico fue correcto.

### 2.3 🔴 Bloqueante — ARN incorrecto para la política gestionada de EBS CSI driver

**Síntoma:** tras resolver el problema de NAT, el `apply` avanzó hasta crear el node group y los addons `coredns`/`kube-proxy`, pero falló en el addon `ebs_csi`:
```
Error: attaching IAM Policy (arn:aws:iam::aws:policy/service-role/AmazonEBSCSIDriverPolicyV2)
to IAM Role (atlas-commerce-dev-ebs-csi-role): operation error IAM: AttachRolePolicy,
https response error StatusCode: 404, NoSuchEntity: Policy ... does not exist or is not attachable.
```

**Causa raíz:** `modules/aws/eks/ebs-csi.tf` línea 62 usaba un **path de ARN incorrecto**:
```hcl
policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEBSCSIDriverPolicyV2"   # ❌ no existe
```
La política gestionada por AWS `AmazonEBSCSIDriverPolicyV2` sí existe, pero vive en el path raíz `/policy/`, no en `/policy/service-role/` (a diferencia de la versión anterior `AmazonEBSCSIDriverPolicy`, sin `V2`, que sí usa `service-role/`). Fácil de confundir porque ambas variantes conviven en AWS con paths distintos.

**Diagnóstico:**
```bash
aws iam list-policies --scope AWS --query "Policies[?contains(PolicyName, 'EBSCSI')].{Name:PolicyName,Arn:Arn}"
# → arn:aws:iam::aws:policy/AmazonEBSCSIDriverPolicyV2   (sin service-role/)
```

**Fix aplicado:**
```hcl
policy_arn = "arn:aws:iam::aws:policy/AmazonEBSCSIDriverPolicyV2"   # ✅ correcto
```

**Ya corregido en `modules/aws/eks/ebs-csi.tf`** — este bug se repetiría en cualquier ambiente nuevo (`staging`, `prod`) que use este módulo hasta este commit.

**Nota operativa sobre el diagnóstico de esta sesión:** el `apply` se ejecutó con `terraform apply tfplan 2>&1 | tee log.txt` sin `set -o pipefail`, así que el código de salida del comando fue el de `tee` (0) y no el de `terraform apply` (el que realmente falló). Esto casi hace pasar el error desapercibido — el monitor automático que vigilaba el log por el string `Error:` fue lo que lo detectó, no el exit code. **Recomendación:** cualquier script de CI/CD que envuelva `terraform apply` en un pipe debe usar `set -o pipefail` (bash) para que el exit code refleje el fallo real de Terraform.

---

## 3. Recomendaciones de cambios en el código Terraform

### 3.1 🔴 Alta — `enable_nat_gateway = false` no es un default viable si hay EKS con node groups en subnets privadas

El módulo de red permite deshabilitar NAT, pero el módulo EKS asume subnets privadas para los nodos. Esta combinación **siempre** va a colgar el `apply` la primera vez que alguien la use, exactamente como pasó aquí. Dos opciones, a elegir una:

- **Opción A (recomendada):** cambiar el default de `enable_nat_gateway` a `true` en `variables.tf` de `live/aws/dev` (y de cualquier otro `live/aws/<env>` que use EKS). Dev cuesta más, pero evita este bloqueo sorpresa a cualquiera que despliegue desde cero.
- **Opción B:** si se quiere mantener NAT deshabilitado por defecto para ahorrar costo, añadir una **validación de Terraform** (`variable "enable_nat_gateway" { validation { ... } }` o un `check` block) que falle el `plan` —no el `apply` a los 16 minutos— si `enable_nat_gateway = false` y el node group EKS está configurado para subnets privadas. Fallar rápido en `plan` es mucho mejor que colgarse en `apply`.

### 3.2 🟠 Media — Sin alternativa de VPC Endpoints documentada

Si el objetivo real es ahorrar el costo de NAT en dev, una alternativa más barata (aunque más compleja de configurar) es usar VPC Endpoints de tipo Gateway/Interface para `s3`, `ecr.api`, `ecr.dkr`, `eks`, `logs`, en vez de NAT completo. No está implementado ni documentado hoy. Si el costo de NAT en dev es una preocupación real del equipo, vale la pena evaluarlo como Fase futura del roadmap — pero no como default silencioso, porque no cubre el 100% de lo que un nodo pueda necesitar (cualquier salida a internet genérica seguiría fallando).

### 3.3 🟡 Baja — Falta guardarraíl para procesos `apply` interrumpidos

Este despliegue dejó un lockfile S3 huérfano y un node group huérfano en AWS al interrumpir el `apply`. Ninguno de los dos es un bug de Terraform (es comportamiento esperado al matar el proceso a mitad de una operación), pero vale la pena documentar en el README de `live/aws/<env>` el procedimiento de recuperación:
```bash
# 1. Liberar el lock si quedó huérfano tras interrumpir un apply:
terraform force-unlock -force <lock-id>   # el ID sale del propio mensaje de error o del archivo .tflock en S3

# 2. Revisar si el recurso que se estaba creando quedó huérfano en AWS
#    (fuera del state, porque Terraform no llegó a registrarlo):
aws eks list-nodegroups --cluster-name <cluster>
# si aparece uno que no está en `terraform state list`, eliminarlo manualmente
# antes de re-aplicar para evitar un error de "ya existe".
```

### 3.5 🟡 Baja — Falta `set -o pipefail` en cualquier automatización futura de `apply`

Ver nota al final de §2.3. Si en el futuro se envuelve `terraform apply`/`plan` en un pipe (CI/CD, scripts locales), asegurar que el exit code no quede enmascarado por el último comando del pipe (`tee`, `grep`, etc.).

### 3.6 🟢 Ya resuelto en este mismo trabajo de sesión (no repetir)

- DynamoDB lock table huérfana (no usada por el locking real, que es el S3 native lockfile) — eliminada del código y de AWS. Ver commits de `bootstrap/aws-backend/main.tf`, `locals.tf`, `variables.tf`, `outputs.tf`.
- `bootstrap/aws-backend/backend.hcl` huérfano y con datos de una cuenta AWS ajena — eliminado (no tenía uso real, el bootstrap usa estado local a propósito).
- Bug bloqueante `backend.tf` del bootstrap con backend S3 parcial (§2.1).
- Node group de EKS colgado por falta de NAT Gateway (§2.2) — `enable_nat_gateway = true` ya aplicado en `terraform.tfvars` de `dev`.
- ARN incorrecto de la política EBS CSI driver (§2.3) — corregido en `modules/aws/eks/ebs-csi.tf`.

---

## 4. Estado al cierre — despliegue completo y exitoso

**`terraform apply` finalizado sin errores.** 74 recursos en el state de `live/aws/dev`.

| Componente | Estado |
|---|---|
| VPC + subnets (2 AZ) + NAT Gateway | ✅ |
| Security groups (ALB, EKS nodes, RDS, Redis) | ✅ |
| EKS cluster (control plane, v1.35) | ✅ ACTIVE |
| EKS node group (2× t3.medium) | ✅ ACTIVE |
| Addons: VPC CNI, CoreDNS, kube-proxy, EBS CSI | ✅ |
| Roles IRSA: Velero, ALB Controller, External Secrets, EBS CSI, VPC CNI | ✅ |
| RDS PostgreSQL 17.10 | ✅ |
| ElastiCache Redis 7.1 (primary + replica) | ✅ |
| Secrets Manager (`atlas-commerce/dev/platform`) | ✅ |
| Velero backup bucket (S3) | ✅ |

**Outputs clave:**
- `vpc_id`: `vpc-07d002536499af346`
- `rds_postgresql_address`: `atlas-commerce-dev-postgresql.clcswuoaik89.eu-central-1.rds.amazonaws.com`
- `redis_primary_endpoint`: `master.atlas-commerce-dev-redis.yxbmzd.euc1.cache.amazonaws.com`
- `platform_secret_arn`: `arn:aws:secretsmanager:eu-central-1:724772086459:secret:atlas-commerce/dev/platform-Tdz7fs`

**Siguiente fase (no iniciada en esta sesión):** conectar `kubectl` al cluster (`aws eks update-kubeconfig`), desplegar Helm charts de las apps (fuera del alcance de Terraform, según el propio README raíz: "Terraform aprovisiona infra; Helm despliega las apps"), configurar ALB Controller y External Secrets Operator dentro del cluster usando los roles IRSA ya creados.
