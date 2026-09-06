# Demo de entrevista: Atlas Commerce GitOps DEV

Guion reproducible de 10-15 minutos para explicar la plataforma sin esperar la
creación de infraestructura durante la entrevista.

## Preparación previa

Para una demo conectada, dejar creado DEV, configurar los valores
`REPLACE_WITH_...` de `platform/argocd/dev/application.yaml` mediante Git y
tener `atlas-dev` sincronizada. Comprobar antes de empezar:

```bash
kubectl get nodes
kubectl get application atlas-dev -n argocd
kubectl get pods -n atlas
```

Preparar una PR pequeña de servicio o usar una ya fusionada. No ejecutar
Terraform dentro de los 15 minutos de demo.

## 0:00-1:30 — Arquitectura y ownership

Abrir `docs/adr/0001-platform-ownership.md` y explicar:

- Terraform posee infraestructura AWS.
- Los scripts de bootstrap instalan External Secrets y Argo CD.
- Argo CD posee los workloads de `atlas-dev`.
- Helm renderiza; no compite con Argo CD.
- CI construye/promueve, pero no tiene kubeconfig.

Punto de conversación: separar owners evita drift y permite reconstruir el
estado desde Git.

## 1:30-3:30 — Cambio y CI de un servicio

Mostrar un caller, por ejemplo `.github/workflows/auth-service-ci.yml`, y el
workflow reutilizable `.github/workflows/reusable-service-ci.yml`.

Flujo esperado:

1. En pull request se compila y verifica sin credenciales AWS.
2. En push protegido a `master` se usa OIDC solo en `publish`.
3. La imagen se construye una vez, se escanea con Trivy y se publica sin
   reconstruirla.
4. El job registra el digest y Trivy bloquea vulnerabilidades CRITICAL.

Salida que conviene señalar en GitHub Actions:

```text
Verify service
Scan image and enforce CRITICAL gate
Published image digest: sha256:...
```

Punto de conversación: la identidad publicada es la misma que fue escaneada.

## 3:30-5:30 — Promoción GitOps

En GitHub Actions, ejecutar manualmente **Promote DEV image** con:

```text
service: auth
image_tag: sha-<commit>-<run-id>-<attempt>
```

El workflow valida el servicio, modifica una sola entrada de
`platform/helm/atlas-commerce/values/images.dev.yaml` y abre una PR contra
`master`.

Punto de conversación: publicar no despliega. La PR es el cambio auditable del
estado deseado y puede revertirse con Git.

## 5:30-7:30 — Argo CD y Helm

Tras fusionar la PR, mostrar el diff de la Application y sincronizarla:

```bash
argocd app diff atlas-dev
argocd app sync atlas-dev
argocd app wait atlas-dev --health --sync
```

Salida esperada:

```text
Name:               atlas-dev
Sync Status:        Synced
Health Status:      Healthy
```

Explicar que `Application/atlas-dev` lee `master`, aplica el orden canónico de
values y usa Helm como motor de render. La sincronización es manual y no tiene
prune automático.

Se puede enseñar el mismo render sin cluster:

```bash
bash scripts/helm/validate-dev.sh
bash scripts/argocd/validate-argocd-dev.sh
```

Salida esperada:

```text
DEV Helm validation passed.
Validated atlas-dev manifests, sync waves and ownership rules offline.
```

## 7:30-9:00 — Secrets sin secretos en Git

Mostrar `platform/helm/atlas-commerce/templates/external-secrets` y explicar:

- Git contiene nombres y referencias, no valores secretos.
- External Secrets Operator lee AWS Secrets Manager mediante IAM.
- Los servicios consumen Secrets de Kubernetes.
- CI no recibe credenciales estáticas ni acceso al cluster.

Comprobación opcional:

```bash
kubectl get externalsecret -n atlas
kubectl get secret -n atlas
```

Salida esperada: ExternalSecrets con estado `SecretSynced`. No mostrar el
contenido de ningún Secret.

## 9:00-11:00 — Acceso privado y smoke

DEV no crea Ingress ni LoadBalancer. Abrir el acceso local:

```bash
kubectl -n atlas port-forward service/gateway-service 18080:80
```

En otra terminal:

```bash
bash scripts/smoke/dev-smoke.sh
```

Salida esperada:

```text
PASS: gateway readiness (/actuator/health/readiness)
PASS: gateway-to-auth route (/api/v1/auth/ping)
Atlas DEV HTTP smoke passed.
```

Punto de conversación: se valida readiness y una llamada real gateway → auth,
no solamente que los pods estén `Ready`.

## 11:00-12:30 — Rollback

Localizar el commit de promoción y crear un revert en una rama:

```bash
git revert <commit-de-la-promocion>
git push origin <rama-de-rollback>
```

Abrir/fusionar la PR y sincronizar nuevamente:

```bash
argocd app sync atlas-dev
argocd app wait atlas-dev --health --sync
```

Punto de conversación: rollback significa volver a declarar en Git la imagen
anterior; no se modifica el Deployment directamente.

## 12:30-15:00 — Destroy y control de costes

No es necesario destruir durante cada entrevista. Mostrar el orden del script:

```bash
bash -n scripts/aws/destroy-dev-now.sh
rg -n "atlas-dev|terraform destroy|cost-control" scripts/aws/destroy-dev-now.sh
```

Cuando realmente se quiera apagar DEV:

```bash
./scripts/aws/destroy-dev-now.sh
```

El script elimina y espera la Application antes del namespace y de Terraform,
limpia huérfanos y termina con el control de costes.

## Plan B: AWS o cluster no disponibles

La historia puede demostrarse completamente offline:

```bash
helm lint platform/helm/atlas-commerce
helm template atlas platform/helm/atlas-commerce -n atlas --debug >/tmp/atlas-base.yaml
bash scripts/helm/validate-dev.sh
bash scripts/argocd/validate-argocd-dev.sh
python scripts/gitops/promote-dev-image.py \
  --service auth \
  --image-tag sha-0000000000000000000000000000000000000000-1-1 \
  --check
bash -n scripts/aws/*.sh scripts/argocd/*.sh scripts/smoke/*.sh
```

Después, recorrer los workflows y manifests en pantalla y enseñar una captura
o ejecución anterior de GitHub Actions/Argo CD. Ser transparente: el smoke real
requiere un endpoint, por lo que sin cluster se muestra su contrato y la prueba
offline, no se afirma que el runtime esté disponible.
