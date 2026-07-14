#!/usr/bin/env bash
set -euo pipefail

RELEASE_NAME="atlas"
NAMESPACE="atlas"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CHART_DIR="$PROJECT_ROOT/platform/helm/atlas-commerce"
TERRAFORM_DIR="$PROJECT_ROOT/platform/terraform/live/aws/dev"

POSTGRES_HOST="$(terraform -chdir="$TERRAFORM_DIR" output -raw rds_postgresql_address)"
REDIS_HOST="$(terraform -chdir="$TERRAFORM_DIR" output -raw redis_primary_endpoint)"
RDS_MASTER_SECRET_ARN="$(terraform -chdir="$TERRAFORM_DIR" output -raw rds_postgresql_master_secret_arn)"
PLATFORM_SECRET_NAME="$(terraform -chdir="$TERRAFORM_DIR" output -raw platform_secret_name)"

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

echo "Deploying Atlas DEV..."
echo "Namespace:       $NAMESPACE"
echo "PostgreSQL RDS:  $POSTGRES_HOST"
echo "Redis endpoint:  $REDIS_HOST"
echo "Platform secret: $PLATFORM_SECRET_NAME"
echo "RDS admin secret: $RDS_MASTER_SECRET_ARN"
echo ""

kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

helm upgrade --install "$RELEASE_NAME" "$CHART_DIR" \
  -n "$NAMESPACE" \
  --create-namespace \
  "${HELM_ARGS[@]}" \
  --set postgres.external.host="$POSTGRES_HOST" \
  --set redis.external.host="$REDIS_HOST" \
  --set postgres.bootstrap.adminSecret.remoteKey="$RDS_MASTER_SECRET_ARN" \
  --set secretManagement.externalSecrets.secrets.jwt.remoteKey="$PLATFORM_SECRET_NAME" \
  --set secretManagement.externalSecrets.secrets.redis.remoteKey="$PLATFORM_SECRET_NAME" \
  --set secretManagement.externalSecrets.secrets.postgres.remoteKey="$PLATFORM_SECRET_NAME"

echo ""
echo "Atlas DEV deploy completed."