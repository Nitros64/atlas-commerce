#!/usr/bin/env bash
set -euo pipefail

RELEASE_NAME="atlas"
NAMESPACE="atlas-helm"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

CHART_DIR="$PROJECT_ROOT/platform/helm/atlas-commerce"

VALUES_FILES=(
  "$CHART_DIR/values/auth.yaml"
  "$CHART_DIR/values/catalog.yaml"
  "$CHART_DIR/values/gateway.yaml"
  "$CHART_DIR/values/kafka.yaml"
  "$CHART_DIR/values/postgres.yaml"
  "$CHART_DIR/values/redis.yaml"
  "$CHART_DIR/values/secrets-local.yaml"
)

echo "Running Helm dry-run..."
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

helm upgrade --install "$RELEASE_NAME" "$CHART_DIR" \
  -n "$NAMESPACE" \
  --create-namespace \
  "${HELM_ARGS[@]}" \
  --dry-run=client

echo
echo "Helm dry-run completed successfully."