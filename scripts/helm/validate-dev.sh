#!/usr/bin/env bash
set -euo pipefail

RELEASE_NAME="atlas"
NAMESPACE="atlas"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CHART_DIR="$PROJECT_ROOT/platform/helm/atlas-commerce"
RENDER_OUTPUT="${TMPDIR:-/tmp}/atlas-dev-rendered.yaml"
TERRAFORM_SHARED_TFVARS="$PROJECT_ROOT/platform/terraform/live/aws/shared/terraform.tfvars.example"
WORKFLOWS_DIR="$PROJECT_ROOT/.github/workflows"
DEV_ECR_REGISTRY="${ECR_REGISTRY:-000000000000.dkr.ecr.eu-central-1.amazonaws.com}"

VALUES_FILES=(
  "$CHART_DIR/values/auth.yaml"
  "$CHART_DIR/values/catalog.yaml"
  "$CHART_DIR/values/cart.yaml"
  "$CHART_DIR/values/pricing.yaml"
  "$CHART_DIR/values/coupon.yaml"
  "$CHART_DIR/values/inventory.yaml"
  "$CHART_DIR/values/payment.yaml"
  "$CHART_DIR/values/order.yaml"
  "$CHART_DIR/values/shipping.yaml"
  "$CHART_DIR/values/notification.yaml"
  "$CHART_DIR/values/audit.yaml"
  "$CHART_DIR/values/gateway.yaml"
  "$CHART_DIR/values/kafka.yaml"
  "$CHART_DIR/values/postgres.yaml"
  "$CHART_DIR/values/redis.yaml"
  "$CHART_DIR/values/ingress.yaml"
  "$CHART_DIR/values.dev.yaml"
  "$CHART_DIR/values/images.dev.yaml"
)

HELM_ARGS=()

for file in "${VALUES_FILES[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "ERROR: values file not found: $file" >&2
    exit 1
  fi

  HELM_ARGS+=("-f" "$file")
done

DEV_SET_ARGS=(
  "--set-string" "global.imageRegistry=$DEV_ECR_REGISTRY"
  "--set" "postgres.external.host=dummy-rds.atlas.local"
  "--set" "redis.external.host=dummy-redis.atlas.local"
  "--set" "redis.external.sslEnabled=true"
  "--set" "catalog.redis.host=dummy-redis.atlas.local"
  "--set" "gateway.env.redisHost=dummy-redis.atlas.local"
  "--set" "postgres.bootstrap.adminSecret.remoteKey=dummy-rds-admin-secret"
  "--set" "secretManagement.externalSecrets.secrets.jwt.remoteKey=dummy-platform-secret"
  "--set" "secretManagement.externalSecrets.secrets.redis.remoteKey=dummy-platform-secret"
  "--set" "secretManagement.externalSecrets.secrets.postgres.remoteKey=dummy-platform-secret"
)

echo "Validating Atlas DEV Helm chart..."
echo "Chart:     $CHART_DIR"
echo "Namespace: $NAMESPACE"
echo ""

echo "Checking Helm chart files..."
helm lint "$CHART_DIR" \
  "${HELM_ARGS[@]}" \
  "${DEV_SET_ARGS[@]}"

echo ""
echo "Rendering Helm templates..."
helm template "$RELEASE_NAME" "$CHART_DIR" \
  -n "$NAMESPACE" \
  "${HELM_ARGS[@]}" \
  "${DEV_SET_ARGS[@]}" \
  > "$RENDER_OUTPUT"

echo "Rendered output:"
echo "$RENDER_OUTPUT"

echo ""
echo "Checking critical DEV runtime values..."

grep -q 'SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE: "4"' "$RENDER_OUTPUT" \
  || {
    echo "ERROR: expected Hikari maximumPoolSize=4 for DEV was not rendered." >&2
    exit 1
  }

grep -q 'SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT: "60000"' "$RENDER_OUTPUT" \
  || {
    echo "ERROR: expected Hikari connectionTimeout=60000 for DEV was not rendered." >&2
    exit 1
  }

grep -q 'SPRING_DATA_REDIS_SSL_ENABLED: "true"' "$RENDER_OUTPUT" \
  || {
    echo "ERROR: expected Redis TLS flag was not rendered." >&2
    exit 1
  }

grep -q 'storageClassName: gp2' "$RENDER_OUTPUT" \
  || {
    echo "ERROR: expected Kafka PVC storageClassName=gp2 for DEV was not rendered." >&2
    exit 1
  }

grep -q 'subPath: kafka' "$RENDER_OUTPUT" \
  || {
    echo "ERROR: expected Kafka volumeMount subPath=kafka was not rendered." >&2
    exit 1
  }

if grep -q '^kind: Ingress$' "$RENDER_OUTPUT"; then
  echo "ERROR: DEV must not render an Ingress without a controller." >&2
  exit 1
fi

max_unavailable_count="$(grep -c 'maxUnavailable: 0' "$RENDER_OUTPUT" || true)"
max_surge_count="$(grep -c 'maxSurge: 1' "$RENDER_OUTPUT" || true)"

if [[ "$max_unavailable_count" -ne 12 || "$max_surge_count" -ne 12 ]]; then
  echo "ERROR: expected rollout strategy maxUnavailable=0/maxSurge=1 for 12 application Deployments." >&2
  exit 1
fi

echo "Validated private DEV access and zero-downtime application rollout settings."

echo ""
echo "Checking canonical DEV service images..."

IMAGE_NAMES=(
  auth-service
  catalog-service
  cart-service
  pricing-service
  coupon-service
  inventory-service
  payment-service
  order-service
  shipping-service
  notification-service
  audit-service
  api-gateway
)

rendered_registry_image_count="$(
  grep -Fc "image: \"$DEV_ECR_REGISTRY/atlas-commerce/" "$RENDER_OUTPUT" || true
)"
terraform_repository_count="$(
  awk '/repository_names = \[/,/^\]/' "$TERRAFORM_SHARED_TFVARS" \
    | grep -Ec '^[[:space:]]*"[^"]+",?[[:space:]]*$' \
    || true
)"
workflow_image_count="$(
  grep -hE 'docker-image-name:[[:space:]]*[A-Za-z0-9-]+[[:space:]]*$' \
    "$WORKFLOWS_DIR"/*-service-ci.yml \
    | wc -l \
    | tr -d '[:space:]'
)"

if [[ "$rendered_registry_image_count" -ne 12 ]]; then
  echo "ERROR: expected exactly 12 ECR service images, found $rendered_registry_image_count." >&2
  exit 1
fi

if [[ "$terraform_repository_count" -ne 12 ]]; then
  echo "ERROR: expected exactly 12 Terraform ECR repositories, found $terraform_repository_count." >&2
  exit 1
fi

if [[ "$workflow_image_count" -ne 12 ]]; then
  echo "ERROR: expected exactly 12 CI workflow images, found $workflow_image_count." >&2
  exit 1
fi

for image_name in "${IMAGE_NAMES[@]}"; do
  expected_prefix="image: \"$DEV_ECR_REGISTRY/atlas-commerce/$image_name:"
  rendered_count="$(grep -Fc "$expected_prefix" "$RENDER_OUTPUT" || true)"

  if [[ "$rendered_count" -ne 1 ]]; then
    echo "ERROR: expected exactly one DEV image for $image_name, found $rendered_count." >&2
    exit 1
  fi

  terraform_count="$(
    grep -Ec "^[[:space:]]*\"$image_name\",?[[:space:]]*$" "$TERRAFORM_SHARED_TFVARS" || true
  )"

  if [[ "$terraform_count" -ne 1 ]]; then
    echo "ERROR: expected exactly one Terraform ECR repository for $image_name, found $terraform_count." >&2
    exit 1
  fi

  workflow_count="$(
    grep -hE "docker-image-name:[[:space:]]*$image_name[[:space:]]*$" \
      "$WORKFLOWS_DIR"/*-service-ci.yml \
      | wc -l \
      | tr -d '[:space:]'
  )"

  if [[ "$workflow_count" -ne 1 ]]; then
    echo "ERROR: expected exactly one CI workflow image for $image_name, found $workflow_count." >&2
    exit 1
  fi
done

if grep -Eq 'image:[[:space:]]*"?nitros64/' "$RENDER_OUTPUT"; then
  echo "ERROR: DEV render contains a public nitros64 application image." >&2
  exit 1
fi

echo "Validated 12 DEV service images across Helm, Terraform and CI."

echo ""
echo "DEV Helm validation passed."
