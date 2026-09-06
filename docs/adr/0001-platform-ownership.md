# ADR 0001: ownership de la plataforma DEV

- Estado: aceptado
- Fecha: 2026-09-04
- Alcance: `atlas-dev`

## Contexto

Atlas DEV puede renderizarse con Helm y reconciliarse con Argo CD. Si ambos
ejecutan cambios sobre los mismos recursos, aparecen dos fuentes de escritura,
drift y rollbacks impredecibles. La infraestructura AWS tiene además un ciclo
de vida distinto al de los workloads Kubernetes.

## Decisión

Cada tipo de recurso tiene un único owner operativo:

| Owner | Responsabilidad |
| --- | --- |
| Terraform | AWS, EKS y capacidad del node group, IAM, RDS, ElastiCache/Redis, Secrets Manager y buckets. |
| Scripts de bootstrap | Kubeconfig, External Secrets Operator, Argo CD y registro inicial de `AppProject`/`Application`. |
| Argo CD | Workloads y recursos de aplicación del namespace `atlas` para `atlas-dev`. |
| Helm | Motor de render de Argo CD y herramienta offline de lint, template y debug. |
| CI | Verifica código/Helm, publica imágenes y abre PRs de promoción; no accede al cluster. |

La capacidad DEV se declara en Terraform (`min = desired = max = 3` nodos
`t3.medium`). Los scripts de bootstrap no escalan EKS mediante AWS CLI. El CI de
Terraform solo ejecuta formato e inicialización sin backend/validación estática;
no recibe credenciales AWS ni ejecuta plan, apply o destroy.

`platform/helm/atlas-commerce` es la fuente de manifests. Argo CD lee el chart
y el estado deseado desde Git. Publicar una imagen no despliega: una promoción
modifica `values/images.dev.yaml` mediante PR.

`scripts/helm/deploy-dev.sh` no es el camino normal. Solo puede usarse durante
un troubleshooting controlado, después de eliminar la Application, y requiere
`ALLOW_DIRECT_HELM_DEV=true`.

## Orden de creación

1. Terraform crea la infraestructura AWS.
2. Bootstrap configura kubeconfig e instala External Secrets Operator.
3. Bootstrap siembra las referencias secretas e instala Argo CD.
4. Se valida Helm y la definición Argo CD sin depender del runtime.
5. Bootstrap aplica el `AppProject`.
6. Tras sustituir los placeholders mediante Git, se aplica la `Application`.
7. Un operador sincroniza manualmente `atlas-dev`; Argo CD renderiza Helm.
8. Se valida el runtime y se ejecuta el smoke HTTP.

No se aplica la Application mientras contenga `REPLACE_WITH_...`: hacerlo
crearía un estado roto. La sincronización permanece manual y sin prune.

## Orden de destrucción

1. Eliminar `Application/atlas-dev` y esperar su eliminación/finalizers.
2. Eliminar el `AppProject`.
3. Eliminar PVCs y namespace `atlas`.
4. Desinstalar los prerrequisitos de bootstrap que correspondan.
5. Ejecutar `terraform destroy` y limpieza de recursos huérfanos.
6. Ejecutar el control final de costes.

Si la Application no termina de eliminarse, el destroy se detiene antes de
Terraform para evitar retirar el cluster mientras Argo CD aún posee recursos.

## Consecuencias

- Hay un único reconciliador de workloads DEV.
- Los rollbacks son auditables mediante `git revert` y posterior sync.
- El primer create requiere completar valores no secretos en Git y un sync
  manual; se prefiere ese paso explícito a modificar estado deseado fuera de
  Git.
- El flujo directo de Helm queda disponible, pero deliberadamente protegido.
