#!/usr/bin/env bash
set -euo pipefail

REGION="eu-central-1"
CLUSTER_NAME="atlas-commerce-dev"
NAMESPACE="external-secrets"
SERVICE_ACCOUNT_NAME="external-secrets"
ROLE_NAME="atlas-commerce-dev-external-secrets-role"

echo "Installing External Secrets Operator for Atlas DEV..."
echo "Region:  $REGION"
echo "Cluster: $CLUSTER_NAME"
echo ""

echo "Updating kubeconfig..."
aws eks update-kubeconfig \
  --region "$REGION" \
  --name "$CLUSTER_NAME" >/dev/null

echo "Resolving External Secrets IRSA role..."
ExternalSecretsRoleArn="$(
  aws iam get-role \
    --role-name "$ROLE_NAME" \
    --query 'Role.Arn' \
    --output text
)"

echo "Role ARN: $ExternalSecretsRoleArn"
echo ""

echo "Adding Helm repo..."
helm repo add external-secrets https://charts.external-secrets.io >/dev/null 2>&1 || true
helm repo update

echo ""
echo "Installing/upgrading External Secrets Operator..."
helm upgrade --install external-secrets external-secrets/external-secrets \
  -n "$NAMESPACE" \
  --create-namespace \
  --set installCRDs=true \
  --set serviceAccount.create=true \
  --set serviceAccount.name="$SERVICE_ACCOUNT_NAME" \
  --set-string "serviceAccount.annotations.eks\.amazonaws\.com/role-arn=$ExternalSecretsRoleArn"

echo ""
echo "Waiting for External Secrets pods..."
kubectl rollout status deployment/external-secrets -n "$NAMESPACE" --timeout=5m
kubectl rollout status deployment/external-secrets-webhook -n "$NAMESPACE" --timeout=5m
kubectl rollout status deployment/external-secrets-cert-controller -n "$NAMESPACE" --timeout=5m

echo ""
echo "External Secrets Operator status:"
kubectl get pods -n "$NAMESPACE"

echo ""
echo "External Secrets Operator installed successfully."