#!/usr/bin/env bash
set -euo pipefail

REGION="eu-central-1"
CLUSTER_NAME="atlas-commerce-dev"
PROJECT="atlas-commerce"

echo "Atlas DEV cost-control check"
echo "Region:  $REGION"
echo "Cluster: $CLUSTER_NAME"
echo ""

echo "EKS clusters:"
aws eks list-clusters \
  --region "$REGION" \
  --query "clusters[?contains(@, '$CLUSTER_NAME')]" \
  --output table || true

echo ""
echo "EC2 instances not terminated:"
aws ec2 describe-instances \
  --region "$REGION" \
  --filters "Name=instance-state-name,Values=pending,running,stopping,stopped" \
  --query "Reservations[].Instances[].{ID:InstanceId,State:State.Name,Type:InstanceType,Name:Tags[?Key=='Name']|[0].Value}" \
  --output table || true

echo ""
echo "EBS volumes:"
aws ec2 describe-volumes \
  --region "$REGION" \
  --query "Volumes[].{ID:VolumeId,Size:Size,State:State,Name:Tags[?Key=='Name']|[0].Value}" \
  --output table || true

echo ""
echo "Elastic IPs:"
aws ec2 describe-addresses \
  --region "$REGION" \
  --query "Addresses[].{PublicIp:PublicIp,AllocationId:AllocationId,AssociationId:AssociationId,Name:Tags[?Key=='Name']|[0].Value}" \
  --output table || true

echo ""
echo "NAT Gateways not deleted:"
aws ec2 describe-nat-gateways \
  --region "$REGION" \
  --filter "Name=state,Values=pending,available,deleting,failed" \
  --query "NatGateways[].{ID:NatGatewayId,State:State,VpcId:VpcId}" \
  --output table || true

echo ""
echo "Load Balancers:"
aws elbv2 describe-load-balancers \
  --region "$REGION" \
  --query "LoadBalancers[].{Name:LoadBalancerName,Type:Type,State:State.Code,DNS:DNSName,VpcId:VpcId}" \
  --output table || true

echo ""
echo "RDS instances:"
aws rds describe-db-instances \
  --region "$REGION" \
  --query "DBInstances[].{ID:DBInstanceIdentifier,Status:DBInstanceStatus,Class:DBInstanceClass}" \
  --output table || true

echo ""
echo "ElastiCache replication groups:"
aws elasticache describe-replication-groups \
  --region "$REGION" \
  --query "ReplicationGroups[].{ID:ReplicationGroupId,Status:Status,NodeType:CacheNodeType}" \
  --output table || true

echo ""
echo "Atlas VPCs:"
aws ec2 describe-vpcs \
  --region "$REGION" \
  --filters "Name=tag:Project,Values=$PROJECT" \
  --query "Vpcs[].{ID:VpcId,State:State,Name:Tags[?Key=='Name']|[0].Value}" \
  --output table || true

echo ""
echo "Atlas/EKS Security Groups:"
aws ec2 describe-security-groups \
  --region "$REGION" \
  --query "SecurityGroups[?contains(GroupName, 'atlas-commerce-dev') || contains(GroupName, 'eks-cluster-sg-atlas-commerce-dev')].{ID:GroupId,Name:GroupName,VpcId:VpcId,Description:Description}" \
  --output table || true

echo ""
echo "Orphan Kubernetes ENIs:"
aws ec2 describe-network-interfaces \
  --region "$REGION" \
  --filters "Name=status,Values=available" \
  --query "NetworkInterfaces[?contains(Description, 'aws-K8S-') || contains(Description, 'atlas-commerce-dev')].{ENI:NetworkInterfaceId,Status:Status,Description:Description,Subnet:SubnetId,Instance:Attachment.InstanceId,RequesterManaged:RequesterManaged}" \
  --output table || true

echo ""
echo "Snapshots owned by this account:"
aws ec2 describe-snapshots \
  --region "$REGION" \
  --owner-ids self \
  --query "Snapshots[].{ID:SnapshotId,Size:VolumeSize,State:State,Description:Description}" \
  --output table || true

echo ""
echo "Atlas Secrets Manager secrets:"
aws secretsmanager list-secrets \
  --region "$REGION" \
  --filters "Key=name,Values=atlas-commerce/dev" \
  --query "SecretList[].{Name:Name,DeletedDate:DeletedDate,ARN:ARN}" \
  --output table || true

echo ""
echo "Atlas S3 buckets:"
aws s3api list-buckets \
  --query "Buckets[?contains(Name, 'atlas-commerce-dev')].{Name:Name,Created:CreationDate}" \
  --output table || true

echo ""
echo "Cost-control check completed."
