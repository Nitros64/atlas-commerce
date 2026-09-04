#!/usr/bin/env bash
set -euo pipefail

ARGOCD_VERSION="v3.4.6"
ARGOCD_NAMESPACE="argocd"
INSTALL_MANIFEST="https://raw.githubusercontent.com/argoproj/argo-cd/${ARGOCD_VERSION}/manifests/install.yaml"

command -v kubectl >/dev/null 2>&1 || {
  echo "ERROR: kubectl is required." >&2
  exit 1
}

echo "Installing or updating Argo CD ${ARGOCD_VERSION} in ${ARGOCD_NAMESPACE}..."

kubectl create namespace "$ARGOCD_NAMESPACE" \
  --dry-run=client \
  -o yaml \
  | kubectl apply -f -

kubectl apply \
  --server-side \
  --force-conflicts \
  -n "$ARGOCD_NAMESPACE" \
  -f "$INSTALL_MANIFEST"

kubectl rollout status deployment \
  --all \
  -n "$ARGOCD_NAMESPACE" \
  --timeout=5m

kubectl rollout status statefulset/argocd-application-controller \
  -n "$ARGOCD_NAMESPACE" \
  --timeout=5m

echo "Argo CD ${ARGOCD_VERSION} is ready. Atlas DEV was not synchronized."
