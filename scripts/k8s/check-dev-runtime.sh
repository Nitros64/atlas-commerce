#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="atlas"

DEPLOYMENTS=(
  audit
  auth
  cart
  catalog
  coupon
  gateway
  inventory
  kafka
  notification
  order
  payment
  pricing
  shipping
)

echo "Checking Atlas DEV runtime..."
echo "Namespace: $NAMESPACE"
echo ""

echo "Checking namespace..."
kubectl get namespace "$NAMESPACE" >/dev/null

echo ""
echo "Checking nodes..."
kubectl get nodes -o wide

echo ""
echo "Checking External Secrets..."
kubectl get externalsecret -n "$NAMESPACE"

echo ""
echo "Checking PostgreSQL bootstrap job..."
kubectl get job postgres-bootstrap -n "$NAMESPACE"

BOOTSTRAP_STATUS="$(
  kubectl get job postgres-bootstrap -n "$NAMESPACE" \
    -o jsonpath='{.status.succeeded}' 2>/dev/null || echo "0"
)"

if [[ "$BOOTSTRAP_STATUS" != "1" ]]; then
  echo "ERROR: postgres-bootstrap job has not completed successfully." >&2
  kubectl describe job postgres-bootstrap -n "$NAMESPACE" || true
  exit 1
fi

echo ""
echo "Checking deployments rollout..."
for app in "${DEPLOYMENTS[@]}"; do
  echo "Checking rollout: $app"
  kubectl rollout status deployment/"$app" -n "$NAMESPACE" --timeout=5m
done

echo ""
echo "Checking pods..."
kubectl get pods -n "$NAMESPACE" -o wide

echo ""
echo "Checking failed pods..."
FAILED_PODS="$(
  kubectl get pods -n "$NAMESPACE" \
    --field-selector=status.phase!=Running,status.phase!=Succeeded \
    --no-headers 2>/dev/null || true
)"

if [[ -n "$FAILED_PODS" ]]; then
  echo "ERROR: Some pods are not Running/Succeeded:"
  echo "$FAILED_PODS"
  exit 1
fi

echo ""
echo "Checking critical runtime env values..."

echo "Auth Hikari:"
kubectl exec -n "$NAMESPACE" deployment/auth -- printenv \
  | grep -E "SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE|SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT"

echo ""
echo "Catalog Redis:"
kubectl exec -n "$NAMESPACE" deployment/catalog -- printenv \
  | grep -E "SPRING_DATA_REDIS_HOST|SPRING_DATA_REDIS_PORT|SPRING_DATA_REDIS_SSL_ENABLED"

echo ""
echo "Gateway Redis:"
kubectl exec -n "$NAMESPACE" deployment/gateway -- printenv \
  | grep -E "SPRING_DATA_REDIS_HOST|SPRING_DATA_REDIS_PORT|SPRING_DATA_REDIS_SSL_ENABLED"

echo ""
echo "Checking services..."
kubectl get svc -n "$NAMESPACE"

echo ""
echo "Atlas DEV runtime check passed."
