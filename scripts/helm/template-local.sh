#!/usr/bin/env bash
set -euo pipefail

RELEASE_NAME="atlas"
NAMESPACE="atlas-helm"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

CHART_DIR="$PROJECT_ROOT/platform/helm/atlas-commerce"
LOCAL_SECRETS_FILE="${LOCAL_SECRETS_FILE:-$CHART_DIR/values/secrets-local.yaml}"

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
  "$CHART_DIR/values.local.yaml"
  "$LOCAL_SECRETS_FILE"
  "$CHART_DIR/values/local-lite.yaml"
)

echo "Rendering Helm chart..."
echo "Release:   $RELEASE_NAME"
echo "Namespace: $NAMESPACE"
echo "Chart:     $CHART_DIR"
echo

HELM_ARGS=()

for file in "${VALUES_FILES[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "ERROR: values file not found: $file"
    exit 1
  fi

  HELM_ARGS+=("-f" "$file")
done

helm lint "$CHART_DIR" \
  "${HELM_ARGS[@]}"

helm template "$RELEASE_NAME" "$CHART_DIR" \
  -n "$NAMESPACE" \
  "${HELM_ARGS[@]}"
