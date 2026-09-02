#!/usr/bin/env bash
set -euo pipefail

RELEASE_NAME="atlas"
NAMESPACE="atlas"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CHART_DIR="$PROJECT_ROOT/platform/helm/atlas-commerce"
RENDER_OUTPUT="${TMPDIR:-/tmp}/atlas-dev-rendered.yaml"

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

echo ""
echo "DEV Helm validation passed."
