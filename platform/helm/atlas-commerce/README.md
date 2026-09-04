# Atlas Commerce Helm chart

This chart is the current source of truth for Kubernetes application
manifests. The manifests under platform/k8s are legacy and are not part of the
supported deployment flow.

## Supported profiles

Only these profiles are currently supported:

- base: safe chart defaults. Every application and infrastructure component is
  disabled. It is used to verify the chart contract.
- local: component values, local overrides, ignored local secrets and the
  local-lite overlay.
- dev: component values followed by values.dev.yaml and
  values/images.dev.yaml. Runtime endpoints, secret identifiers and the ECR
  registry are supplied by the dev validation/deployment scripts.

QA and production are roadmap placeholders. Their values files are intentionally
not part of the supported or validated matrix.

## Canonical values order

Helm loads values.yaml automatically. Additional files must be applied from
least specific to most specific.

The common component order is:

1. values/auth.yaml
2. values/catalog.yaml
3. values/cart.yaml
4. values/pricing.yaml
5. values/coupon.yaml
6. values/inventory.yaml
7. values/payment.yaml
8. values/order.yaml
9. values/shipping.yaml
10. values/notification.yaml
11. values/audit.yaml
12. values/gateway.yaml
13. values/kafka.yaml
14. values/postgres.yaml
15. values/redis.yaml
16. values/ingress.yaml

Local then appends:

17. values.local.yaml
18. values/secrets-local.yaml
19. values/local-lite.yaml

Dev appends:

17. values.dev.yaml
18. values/images.dev.yaml

The image overlay stores portable repository paths and immutable tags. Dev
scripts set global.imageRegistry. Template and validation scripts use a
deterministic placeholder ECR registry unless ECR_REGISTRY is provided.
deploy-dev.sh derives the real registry from the active AWS account and
AWS_REGION, both of which can also be supplied explicitly.

`values/local-lite.yaml` is intentionally a scale-to-zero overlay. It keeps
the payment, shipping and audit Services, ConfigMaps and Secrets rendered, but
sets their Deployment replicas to zero; it does not remove those components.

DEV explicitly disables the nginx Ingress because this repository does not
install an ingress controller. Use a local port-forward to the ClusterIP
gateway as documented in `docs/runbooks/atlas-dev-eks-runtime.md`.

## Validation

Base:

    helm lint platform/helm/atlas-commerce
    helm template atlas platform/helm/atlas-commerce -n atlas

Local:

    bash scripts/helm/template-local.sh
    powershell -File scripts/helm/template-local.ps1

Local validation requires the ignored
platform/helm/atlas-commerce/values/secrets-local.yaml file.
CI overrides LOCAL_SECRETS_FILE with the tracked, non-sensitive
values/secrets-local.example.yaml fixture.

Dev:

    bash scripts/helm/validate-dev.sh
    bash scripts/helm/template-dev.sh
    powershell -File scripts/helm/template-dev.ps1
