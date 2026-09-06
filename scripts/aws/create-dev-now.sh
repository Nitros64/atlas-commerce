#!/usr/bin/env bash
set -euo pipefail

REGION="eu-central-1"
CLUSTER_NAME="atlas-commerce-dev"
NODEGROUP_NAME="default"
DESIRED_NODES="3"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
TERRAFORM_DIR="$PROJECT_ROOT/platform/terraform/live/aws/dev"
PLAN_FILE="$TERRAFORM_DIR/eks.tfplan"
ARGOCD_DIR="$PROJECT_ROOT/platform/argocd/dev"

echo "Creating Atlas DEV environment..."
echo "Region:  $REGION"
echo "Cluster: $CLUSTER_NAME"
echo ""

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "ERROR: required command not found: $1" >&2
    exit 1
  fi
}

wait_for_ready_nodes() {
  echo ""
  echo "Waiting for at least $DESIRED_NODES Ready nodes..."

  for i in {1..40}; do
    READY_COUNT="$(
      kubectl get nodes --no-headers 2>/dev/null \
        | awk '$2 == "Ready" { count++ } END { print count+0 }'
    )"

    echo "Ready nodes: $READY_COUNT/$DESIRED_NODES"

    if [[ "$READY_COUNT" -ge "$DESIRED_NODES" ]]; then
      return
    fi

    sleep 15
  done

  echo "ERROR: expected $DESIRED_NODES Ready nodes, but timeout was reached." >&2
  kubectl get nodes -o wide || true
  exit 1
}

echo "Checking required tools..."
require_command aws
require_command terraform
require_command kubectl
require_command helm

echo ""
echo "Checking AWS identity..."
aws sts get-caller-identity --output table

echo ""
echo "Cleaning stale Terraform plan..."
rm -f "$PLAN_FILE"

echo ""
echo "Handling scheduled-for-deletion platform secret if needed..."
if aws secretsmanager describe-secret \
  --region "$REGION" \
  --secret-id atlas-commerce/dev/platform >/tmp/atlas-platform-secret.json 2>/dev/null; then

  DELETED_DATE="$(
    python - <<'PY'
import json
from pathlib import Path

data = json.loads(Path("/tmp/atlas-platform-secret.json").read_text())
print(data.get("DeletedDate", ""))
PY
  )"

  if [[ -n "$DELETED_DATE" ]]; then
    echo "Platform secret is scheduled for deletion. Force deleting it for DEV..."

    aws secretsmanager restore-secret \
      --region "$REGION" \
      --secret-id atlas-commerce/dev/platform >/dev/null 2>&1 || true

    aws secretsmanager delete-secret \
      --region "$REGION" \
      --secret-id atlas-commerce/dev/platform \
      --force-delete-without-recovery >/dev/null 2>&1 || true
  else
    echo "Platform secret exists and is active. Leaving it untouched."
  fi
else
  echo "Platform secret does not exist yet. Terraform will create it."
fi

echo ""
echo "Running Terraform plan..."
terraform -chdir="$TERRAFORM_DIR" plan -out="$PLAN_FILE"

echo ""
echo "Applying Terraform plan..."
terraform -chdir="$TERRAFORM_DIR" apply "$PLAN_FILE"

echo ""
echo "Updating kubeconfig..."
aws eks update-kubeconfig \
  --region "$REGION" \
  --name "$CLUSTER_NAME"

echo ""
echo "Installing External Secrets Operator..."
"$PROJECT_ROOT/scripts/helm/install-external-secrets-dev.sh"

echo ""
echo "Seeding DEV platform secret..."
"$PROJECT_ROOT/scripts/aws/seed-dev-platform-secret.sh"

echo ""
echo "Scaling EKS nodegroup before the Atlas Argo CD sync..."
aws eks update-nodegroup-config \
  --region "$REGION" \
  --cluster-name "$CLUSTER_NAME" \
  --nodegroup-name "$NODEGROUP_NAME" \
  --scaling-config minSize=1,maxSize="$DESIRED_NODES",desiredSize="$DESIRED_NODES"

echo ""
echo "Waiting for nodegroup to become active..."
aws eks wait nodegroup-active \
  --region "$REGION" \
  --cluster-name "$CLUSTER_NAME" \
  --nodegroup-name "$NODEGROUP_NAME"

wait_for_ready_nodes

echo ""
echo "Validating Helm DEV render..."
"$PROJECT_ROOT/scripts/helm/validate-dev.sh"

echo ""
echo "Installing Argo CD..."
"$PROJECT_ROOT/scripts/argocd/install-argocd-dev.sh"

echo ""
echo "Validating Argo CD DEV manifests..."
"$PROJECT_ROOT/scripts/argocd/validate-argocd-dev.sh"

echo ""
echo "Applying the atlas-dev AppProject..."
kubectl apply -f "$ARGOCD_DIR/app-project.yaml"

echo ""
if grep -q 'REPLACE_WITH_' "$ARGOCD_DIR/application.yaml"; then
  echo "Atlas DEV prerequisites are ready, but the Application still has placeholders."
  echo "Update platform/argocd/dev/application.yaml through Git, then run:"
  echo "  kubectl apply -f platform/argocd/dev/application.yaml"
  echo "  argocd app sync atlas-dev"
  echo "The Application was not applied and no direct Helm deployment was performed."
else
  echo "Applying the atlas-dev Application..."
  kubectl apply -f "$ARGOCD_DIR/application.yaml"
  echo "Application registered. Synchronize it manually with:"
  echo "  argocd app sync atlas-dev"
fi

echo ""
echo "Atlas DEV infrastructure and GitOps prerequisites created successfully."
