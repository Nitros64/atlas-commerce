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

cleanup_kubernetes() {
  echo "Trying to clean Kubernetes resources first..."

  aws eks update-kubeconfig \
    --region "$REGION" \
    --name "$CLUSTER_NAME" >/dev/null

  if kubectl get namespace atlas >/dev/null 2>&1; then
    echo ""
    echo "Uninstalling Atlas Helm release..."
    helm uninstall atlas -n atlas --wait --timeout 10m || true

    echo ""
    echo "Deleting remaining PVCs in atlas namespace..."
    kubectl delete pvc --all -n atlas \
      --ignore-not-found \
      --wait=true \
      --timeout=10m || true

    echo ""
    echo "Deleting atlas namespace..."
    kubectl delete namespace atlas \
      --ignore-not-found \
      --wait=true \
      --timeout=10m || true
  else
    echo "Namespace atlas not found. Skipping Atlas cleanup."
  fi

  if kubectl get namespace external-secrets >/dev/null 2>&1; then
    echo ""
    echo "Uninstalling External Secrets Operator..."
    helm uninstall external-secrets -n external-secrets --wait --timeout 10m || true

    echo ""
    echo "Deleting external-secrets namespace..."
    kubectl delete namespace external-secrets \
      --ignore-not-found \
      --wait=true \
      --timeout=10m || true
  else
    echo "Namespace external-secrets not found. Skipping External Secrets cleanup."
  fi
}

cleanup_orphan_ebs_volumes() {
  echo ""
  echo "Checking for orphan Atlas EBS volumes..."

  ORPHAN_VOLUMES="$(
    aws ec2 describe-volumes \
      --region "$REGION" \
      --filters \
        "Name=status,Values=available" \
        "Name=tag:Name,Values=atlas-commerce-dev-dynamic-pvc*" \
      --query "Volumes[].VolumeId" \
      --output text
  )"

  if [[ -z "$ORPHAN_VOLUMES" ]]; then
    echo "No orphan Atlas EBS volumes found."
    return
  fi

  echo "Found orphan EBS volumes:"
  echo "$ORPHAN_VOLUMES"

  for volume_id in $ORPHAN_VOLUMES; do
    echo "Deleting orphan EBS volume: $volume_id"
    aws ec2 delete-volume \
      --region "$REGION" \
      --volume-id "$volume_id"
  done
}

delete_platform_secret() {
  echo ""
  echo "Force deleting dev platform secret if AWS left it scheduled for deletion..."

  aws secretsmanager restore-secret \
    --region "$REGION" \
    --secret-id atlas-commerce/dev/platform >/dev/null 2>&1 || true

  aws secretsmanager delete-secret \
    --region "$REGION" \
    --secret-id atlas-commerce/dev/platform \
    --force-delete-without-recovery >/dev/null 2>&1 || true
}

print_cost_control_check() {
  echo ""
  echo "Cost-control check:"
  echo ""

  echo "EBS volumes:"
  aws ec2 describe-volumes \
    --region "$REGION" \
    --query "Volumes[].{ID:VolumeId,Size:Size,State:State,Name:Tags[?Key=='Name']|[0].Value}" \
    --output table || true

  echo ""
  echo "EC2 instances:"
  aws ec2 describe-instances \
    --region "$REGION" \
    --query "Reservations[].Instances[].{ID:InstanceId,State:State.Name,Type:InstanceType,Name:Tags[?Key=='Name']|[0].Value}" \
    --output table || true

  echo ""
  echo "NAT Gateways:"
  aws ec2 describe-nat-gateways \
    --region "$REGION" \
    --query "NatGateways[].{ID:NatGatewayId,State:State,VpcId:VpcId}" \
    --output table || true

  echo ""
  echo "Load Balancers:"
  aws elbv2 describe-load-balancers \
    --region "$REGION" \
    --query "LoadBalancers[].{Name:LoadBalancerName,Type:Type,State:State.Code,DNS:DNSName}" \
    --output table || true

  echo ""
  echo "RDS:"
  aws rds describe-db-instances \
    --region "$REGION" \
    --query "DBInstances[].{ID:DBInstanceIdentifier,Status:DBInstanceStatus,Class:DBInstanceClass}" \
    --output table || true

  echo ""
  echo "ElastiCache:"
  aws elasticache describe-replication-groups \
    --region "$REGION" \
    --query "ReplicationGroups[].{ID:ReplicationGroupId,Status:Status,NodeType:CacheNodeType}" \
    --output table || true
}

cleanup_orphan_k8s_enis() {
  echo ""
  echo "Checking for orphan Kubernetes ENIs..."

  ORPHAN_ENIS="$(
    aws ec2 describe-network-interfaces \
      --region "$REGION" \
      --filters "Name=status,Values=available" \
      --query "NetworkInterfaces[?contains(Description, 'aws-K8S-')].NetworkInterfaceId" \
      --output text
  )"

  if [[ -z "$ORPHAN_ENIS" ]]; then
    echo "No orphan Kubernetes ENIs found."
    return
  fi

  echo "Found orphan Kubernetes ENIs:"
  echo "$ORPHAN_ENIS"

  for eni_id in $ORPHAN_ENIS; do
    echo "Deleting orphan Kubernetes ENI: $eni_id"
    aws ec2 delete-network-interface \
      --region "$REGION" \
      --network-interface-id "$eni_id" || true
  done
}

if aws eks describe-cluster --region "$REGION" --name "$CLUSTER_NAME" >/dev/null 2>&1; then
  cleanup_kubernetes
else
  echo "Cluster not reachable or already deleted. Skipping Kubernetes cleanup."
fi

cleanup_orphan_k8s_enis

echo ""
echo "Running terraform destroy..."
terraform -chdir="$TERRAFORM_DIR" destroy -auto-approve

delete_platform_secret
cleanup_orphan_ebs_volumes

echo ""
echo "Running final cost-control check..."
"$SCRIPT_DIR/cost-control-check-dev.sh"

echo ""
echo "DEV destroy completed."