#!/usr/bin/env bash
set -euo pipefail

REGION="eu-central-1"
CLUSTER_NAME="atlas-commerce-dev"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
TERRAFORM_DIR="$PROJECT_ROOT/platform/terraform/live/aws/dev"

echo "Destroying Atlas DEV environment..."
echo "Region:  $REGION"
echo "Cluster: $CLUSTER_NAME"
echo ""

echo "Trying to clean Helm releases first..."
if aws eks describe-cluster --region "$REGION" --name "$CLUSTER_NAME" >/dev/null 2>&1; then
  aws eks update-kubeconfig --region "$REGION" --name "$CLUSTER_NAME" >/dev/null

  helm uninstall atlas -n atlas || true
  helm uninstall external-secrets -n external-secrets || true

  kubectl delete namespace atlas --wait=false --ignore-not-found || true
  kubectl delete namespace external-secrets --wait=false --ignore-not-found || true
else
  echo "Cluster not reachable or already deleted. Skipping Kubernetes cleanup."
fi

echo ""
echo "Running terraform destroy..."
terraform -chdir="$TERRAFORM_DIR" destroy -auto-approve

echo ""
echo "Force deleting dev platform secret if AWS left it scheduled for deletion..."
aws secretsmanager restore-secret \
  --region "$REGION" \
  --secret-id atlas-commerce/dev/platform >/dev/null 2>&1 || true

aws secretsmanager delete-secret \
  --region "$REGION" \
  --secret-id atlas-commerce/dev/platform \
  --force-delete-without-recovery >/dev/null 2>&1 || true

echo ""
echo "DEV destroy completed."
