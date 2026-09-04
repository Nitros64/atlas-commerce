#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ARGOCD_DIR="$PROJECT_ROOT/platform/argocd/dev"
CHART_DIR="$PROJECT_ROOT/platform/helm/atlas-commerce"
ARGO_RENDER="${TMPDIR:-/tmp}/atlas-argocd-dev.yaml"
HELM_RENDER="${TMPDIR:-/tmp}/atlas-argocd-helm.yaml"

command -v kubectl >/dev/null 2>&1 || {
  echo "ERROR: kubectl with kustomize support is required." >&2
  exit 1
}

command -v helm >/dev/null 2>&1 || {
  echo "ERROR: helm is required." >&2
  exit 1
}

kubectl kustomize "$ARGOCD_DIR" > "$ARGO_RENDER"

application_count="$(awk '$1 == "kind:" && $2 == "Application" { count++ } END { print count + 0 }' "$ARGO_RENDER")"
project_count="$(awk '$1 == "kind:" && $2 == "AppProject" { count++ } END { print count + 0 }' "$ARGO_RENDER")"

[[ "$application_count" -eq 1 ]] || {
  echo "ERROR: expected exactly one Argo CD Application, found $application_count." >&2
  exit 1
}

[[ "$project_count" -eq 1 ]] || {
  echo "ERROR: expected exactly one Argo CD AppProject, found $project_count." >&2
  exit 1
}

grep -q '^  name: atlas-dev$' "$ARGO_RENDER" || {
  echo "ERROR: atlas-dev metadata name was not rendered." >&2
  exit 1
}

if grep -Eq '^kind: (ApplicationSet|ImageUpdater)$' "$ARGO_RENDER"; then
  echo "ERROR: ApplicationSet and Image Updater are outside this delivery." >&2
  exit 1
fi

VALUES_FILES=(
  values/auth.yaml
  values/catalog.yaml
  values/cart.yaml
  values/pricing.yaml
  values/coupon.yaml
  values/inventory.yaml
  values/payment.yaml
  values/order.yaml
  values/shipping.yaml
  values/notification.yaml
  values/audit.yaml
  values/gateway.yaml
  values/kafka.yaml
  values/postgres.yaml
  values/redis.yaml
  values/ingress.yaml
  values.dev.yaml
  values/images.dev.yaml
)

actual_value_files="$(
  sed -n '/^[[:space:]]*valueFiles:/,/^[[:space:]]*values: |/p' \
    "$ARGOCD_DIR/application.yaml" \
    | sed -n 's/^[[:space:]]*- //p'
)"
expected_value_files="$(printf '%s\n' "${VALUES_FILES[@]}")"

if [[ "$actual_value_files" != "$expected_value_files" ]]; then
  echo "ERROR: atlas-dev does not use the canonical DEV values order." >&2
  diff -u \
    <(printf '%s\n' "$expected_value_files") \
    <(printf '%s\n' "$actual_value_files") \
    || true
  exit 1
fi

if grep -q '^[[:space:]]*automated:' "$ARGOCD_DIR/application.yaml"; then
  echo "ERROR: automatic synchronization is not allowed in this delivery." >&2
  exit 1
fi

HELM_ARGS=()
for file in "${VALUES_FILES[@]}"; do
  HELM_ARGS+=("-f" "$CHART_DIR/$file")
done

helm template atlas "$CHART_DIR" \
  -n atlas \
  "${HELM_ARGS[@]}" \
  --set-string global.imageRegistry=000000000000.dkr.ecr.eu-central-1.amazonaws.com \
  --set-string postgres.external.host=dummy-rds.atlas.local \
  --set-string redis.external.host=dummy-redis.atlas.local \
  --set-string catalog.redis.host=dummy-redis.atlas.local \
  --set-string gateway.env.redisHost=dummy-redis.atlas.local \
  --set-string postgres.bootstrap.adminSecret.remoteKey=dummy-rds-admin-secret \
  > "$HELM_RENDER"

assert_wave_count() {
  wave="$1"
  expected="$2"
  actual="$(grep -c "argocd.argoproj.io/sync-wave: \"$wave\"" "$HELM_RENDER" || true)"

  [[ "$actual" -eq "$expected" ]] || {
    echo "ERROR: expected $expected resources in sync wave $wave, found $actual." >&2
    exit 1
  }
}

assert_wave_count -30 1
assert_wave_count -20 16
assert_wave_count -10 1
assert_wave_count 10 5
assert_wave_count 20 11
assert_wave_count 30 2

grep -q 'argocd.argoproj.io/hook: Sync' "$HELM_RENDER"
grep -q 'argocd.argoproj.io/hook-delete-policy: BeforeHookCreation' "$HELM_RENDER"
grep -q 'name: wait-for-bootstrap-secrets' "$HELM_RENDER"
grep -q 'Timed out waiting for Secret' "$HELM_RENDER"

echo "Validated one atlas-dev Application, one restricted AppProject and Helm sync waves offline."
