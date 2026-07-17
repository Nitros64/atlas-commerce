# Atlas Commerce DEV Runtime on EKS

This runbook explains how to create, validate, and destroy the **Atlas Commerce DEV** environment on AWS using the automated workflow provided by this repository.

Atlas DEV uses:

- Terraform for AWS infrastructure.
- EKS as the Kubernetes cluster.
- External RDS PostgreSQL.
- External ElastiCache Redis with TLS.
- AWS Secrets Manager.
- External Secrets Operator.
- Helm to deploy Atlas Commerce.
- Kafka inside the cluster with an EBS-backed PVC.

> This is a development environment and may generate AWS costs. Destroy it when you finish testing.

---

## 1. Requirements

Required tools:

```bash
aws --version
terraform version
kubectl version --client
helm version
openssl version
```

Expected AWS context:

```bash
aws sts get-caller-identity
```

DEV values:

```text
Region:    eu-central-1
Cluster:   atlas-commerce-dev
Namespace: atlas
```

---

## 2. Recommended workflow

The main way to operate DEV is through the scripts. Manual commands are kept only for troubleshooting.

### Create the full DEV environment

```bash
./scripts/aws/create-dev-now.sh
```

This script runs the full lifecycle:

1. Cleans old Terraform plan files.
2. Handles the platform secret if it was left `scheduled for deletion`.
3. Runs `terraform plan/apply`.
4. Updates `kubeconfig`.
5. Installs External Secrets Operator.
6. Seeds the platform secret in AWS Secrets Manager.
7. Scales the DEV node group to 3 nodes.
8. Validates the DEV Helm render.
9. Deploys Atlas with Helm.
10. Runs the runtime smoke check.

### Validate the runtime

```bash
./scripts/k8s/check-dev-runtime.sh
```

This validates:

- Namespace `atlas`.
- Kubernetes nodes.
- ExternalSecrets.
- `postgres-bootstrap` job.
- Rollout of all deployments.
- Pod status.
- Services.
- Critical Redis TLS environment variables.
- Critical Hikari/RDS environment variables.

### Destroy DEV

```bash
./scripts/aws/destroy-dev-now.sh
```

The script attempts to clean up:

- Helm release `atlas`.
- External Secrets Operator.
- PVCs/PVs.
- Namespaces.
- Terraform resources.
- Platform secret if it was left `scheduled for deletion`.
- Orphan EBS volumes.
- Orphan Kubernetes ENIs.
- Orphan EKS Security Groups.

If `terraform destroy` fails because of subnet/VPC dependencies, the script runs orphan cleanup and retries the destroy.

### Validate zero-cost state

```bash
./scripts/aws/cost-control-check-dev.sh
```

After destroying DEV, this check should return empty results or no Atlas-related resources in:

- EKS.
- EC2.
- EBS.
- Elastic IPs.
- NAT Gateways.
- Load Balancers.
- RDS.
- ElastiCache.
- VPCs.
- Security Groups.
- Orphan ENIs.
- Snapshots.
- Secrets Manager.
- S3 buckets.

---

## 3. Main scripts

### AWS

```text
scripts/aws/create-dev-now.sh
scripts/aws/destroy-dev-now.sh
scripts/aws/cost-control-check-dev.sh
scripts/aws/seed-dev-platform-secret.sh
```

### Helm

```text
scripts/helm/deploy-dev.sh
scripts/helm/install-external-secrets-dev.sh
scripts/helm/validate-dev.sh
```

### Kubernetes

```text
scripts/k8s/check-dev-runtime.sh
```

---

## 4. DEV Helm validation

To validate Helm without deploying:

```bash
./scripts/helm/validate-dev.sh
```

This script runs:

- `helm lint`.
- `helm template`.
- Redis TLS validation.
- DEV Hikari validation.
- Kafka PVC `gp2` validation.
- `subPath: kafka` validation.

Expected result:

```text
DEV Helm validation passed.
```

---

## 5. Expected runtime status

Expected pods:

```text
audit          1/1 Running
auth           1/1 Running
cart           1/1 Running
catalog        1/1 Running
coupon         1/1 Running
gateway        1/1 Running
inventory      1/1 Running
kafka          1/1 Running
notification   1/1 Running
order          1/1 Running
payment        1/1 Running
pricing        1/1 Running
shipping       1/1 Running
```

Expected services:

```text
gateway-service
auth-service
catalog-service
cart-service
order-service
payment-service
shipping-service
notification-service
audit-service
kafka
```

PostgreSQL should appear as `ExternalName` services pointing to the RDS endpoint:

```text
postgres-auth
postgres-catalog
postgres-order
...
```

---

## 6. PostgreSQL bootstrap

The `postgres-bootstrap` job creates these databases:

```text
auditdb
authdb
cartdb
catalogdb
coupondb
inventorydb
notificationdb
orderdb
paymentdb
shippingdb
```

Validate:

```bash
kubectl get job postgres-bootstrap -n atlas
kubectl logs -n atlas -l app=postgres-bootstrap --all-containers=true --tail=100
```

Expected status:

```text
postgres-bootstrap Complete 1/1
PostgreSQL bootstrap completed
```

---

## 7. Critical DEV configuration

### Redis TLS

ElastiCache Redis in DEV uses TLS. Applications must have:

```text
SPRING_DATA_REDIS_SSL_ENABLED=true
```

Validate:

```bash
kubectl exec -n atlas deployment/catalog -- printenv | grep SPRING_DATA_REDIS
kubectl exec -n atlas deployment/gateway -- printenv | grep SPRING_DATA_REDIS
```

### Hikari/RDS

DEV uses a small RDS instance, so Hikari pools must be limited:

```text
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=4
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=0
SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT=60000
```

Validate:

```bash
kubectl exec -n atlas deployment/auth -- printenv | grep SPRING_DATASOURCE_HIKARI
```

### Kafka PVC

On EKS, the StorageClass must be:

```text
gp2
```

Kafka must mount the volume with `subPath` to avoid the `lost+found` error:

```yaml
volumeMounts:
  - name: kafka-data
    mountPath: /var/lib/kafka/data
    subPath: kafka
```

---

## 8. Troubleshooting

### ExternalSecret does not exist

Symptom:

```text
no matches for kind "ExternalSecret"
no matches for kind "ClusterSecretStore"
```

Cause:

```text
External Secrets Operator is not installed in the new cluster.
```

Solution:

```bash
./scripts/helm/install-external-secrets-dev.sh
```

---

### Secret scheduled for deletion

Symptom:

```text
You can't create this secret because a secret with this name is already scheduled for deletion
```

DEV solution:

```bash
aws secretsmanager restore-secret \
  --region eu-central-1 \
  --secret-id atlas-commerce/dev/platform

aws secretsmanager delete-secret \
  --region eu-central-1 \
  --secret-id atlas-commerce/dev/platform \
  --force-delete-without-recovery
```

Then generate a new plan:

```bash
rm -f platform/terraform/live/aws/dev/eks.tfplan
terraform -chdir=platform/terraform/live/aws/dev plan -out=eks.tfplan
terraform -chdir=platform/terraform/live/aws/dev apply eks.tfplan
```

---

### Pods Pending because of memory

Symptom:

```text
Insufficient memory
```

Solution:

```bash
aws eks update-nodegroup-config \
  --region eu-central-1 \
  --cluster-name atlas-commerce-dev \
  --nodegroup-name default \
  --scaling-config minSize=1,maxSize=3,desiredSize=3
```

---

### Kafka PVC Pending

Symptom:

```text
pod has unbound immediate PersistentVolumeClaims
```

Cause:

```text
Wrong StorageClass for EKS.
```

Solution:

```bash
./scripts/helm/validate-dev.sh
```

In DEV, Helm must render:

```text
storageClassName: gp2
```

---

### Kafka lost+found

Symptom:

```text
Found directory /var/lib/kafka/data/lost+found
Kafka's log directories should only contain Kafka topic data
```

Solution:

```text
Use subPath: kafka in the Kafka volumeMount.
```

---

### Redis timeout

Symptom:

```text
Unable to connect to Redis
Connection initialization timed out
```

Cause:

```text
ElastiCache Redis has TLS enabled.
```

Solution:

```text
SPRING_DATA_REDIS_SSL_ENABLED=true
```

---

### RDS remaining connection slots

Symptom:

```text
FATAL: remaining connection slots are reserved for roles with privileges of the "pg_use_reserved_connections" role
```

Cause:

```text
Too many microservices opening large Hikari pools against a small RDS instance.
```

Current DEV solution:

```text
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=4
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=0
SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT=60000
```

---

### HikariPool timeout with pool size 2

Symptom:

```text
HikariPool-1 - Connection is not available, request timed out
total=2, active=2, idle=0
```

Cause:

```text
Pool size 2 is too low during startup with Flyway + JPA.
```

Solution:

```text
Use maximumPoolSize=4 in DEV.
```

---

### ElastiCache stuck in creating

Symptom:

```text
atlas-commerce-dev-redis creating
Endpoint None
Nodes None
```

Check:

```bash
aws elasticache describe-replication-groups \
  --region eu-central-1 \
  --replication-group-id atlas-commerce-dev-redis \
  --query "ReplicationGroups[0].{Status:Status,Endpoint:NodeGroups[0].PrimaryEndpoint.Address,Members:MemberClusters}" \
  --output table
```

If it becomes `available`, it can be deleted:

```bash
aws elasticache delete-replication-group \
  --region eu-central-1 \
  --replication-group-id atlas-commerce-dev-redis \
  --no-retain-primary-cluster
```

---

### DependencyViolation when deleting a subnet

Symptom:

```text
DependencyViolation: The subnet has dependencies and cannot be deleted.
```

Common cause:

```text
Orphan Kubernetes/EKS ENI.
```

Detect:

```bash
./scripts/aws/cost-control-check-dev.sh
```

If the ENI is `available`, `Instance=None`, and `RequesterManaged=False`, it can be deleted:

```bash
aws ec2 delete-network-interface \
  --region eu-central-1 \
  --network-interface-id <eni-id>
```

`destroy-dev-now.sh` attempts to clean this case automatically and retry `terraform destroy`.

---

### DependencyViolation when deleting the VPC

Symptom:

```text
DependencyViolation: The vpc has dependencies and cannot be deleted.
```

Common cause:

```text
Orphan EKS Security Group.
```

Detect:

```bash
./scripts/aws/cost-control-check-dev.sh
```

If only the EKS security group remains and the cluster no longer exists:

```bash
aws ec2 delete-security-group \
  --region eu-central-1 \
  --group-id <sg-id>
```

The VPC `default` security group must not be deleted manually. It disappears when the VPC is deleted.

---

## 9. Manual reference commands

The recommended workflow is to use the scripts. These commands are only fallback references.

### Manual Terraform

```bash
rm -f platform/terraform/live/aws/dev/eks.tfplan
terraform -chdir=platform/terraform/live/aws/dev plan -out=eks.tfplan
terraform -chdir=platform/terraform/live/aws/dev apply eks.tfplan
```

### Manual kubeconfig

```bash
aws eks update-kubeconfig \
  --region eu-central-1 \
  --name atlas-commerce-dev
```

### Manual Helm deploy

```bash
./scripts/helm/deploy-dev.sh
```

### Manual destroy

```bash
terraform -chdir=platform/terraform/live/aws/dev destroy -auto-approve
```

Always prefer:

```bash
./scripts/aws/destroy-dev-now.sh
```
