# Atlas Commerce - AWS Development Environment

Terraform root module for the Atlas Commerce development environment in AWS Frankfurt (`eu-central-1`).

## What This Environment Creates

- VPC and public/private subnets across two Availability Zones
- EKS control plane and one managed node group
- RDS PostgreSQL and ElastiCache Redis
- Secrets Manager entries and IAM roles used by the platform
- An S3 bucket reserved for a future Velero integration
- No NAT Gateway by default

## State Backend

Terraform state is stored remotely in the shared S3 backend:

```text
atlas-commerce/dev/terraform.tfstate
```

State locking uses S3 lockfiles.

## Terraform ownership

Terraform is the only owner of AWS infrastructure and EKS node capacity. The
tracked DEV contract is three `t3.medium` nodes (`min = desired = max = 3`).
Bootstrap scripts may inspect the nodes, but must not resize the node group with
the AWS CLI. Change the Terraform variables, review a plan, and apply it when a
different capacity is required.

`terraform.tfvars` is ignored by Git and can override these values locally. Keep
it aligned with `terraform.tfvars.example` before creating DEV.

## Network Design

```text
Internet
   |
Internet Gateway
   |
Public Subnets
   |
Private Subnets
```

Public subnets have a default route to the Internet Gateway.

Private subnets have no outbound Internet route while `enable_nat_gateway = false`.

## Usage

```powershell
cd platform\terraform\live\aws\dev

Copy-Item terraform.tfvars.example terraform.tfvars

terraform init -backend-config="backend.hcl"
terraform fmt
terraform validate
terraform plan
```

Apply reviewed changes:

```powershell
terraform plan -out tfplan
terraform apply "tfplan"
```

Verify the deployed infrastructure:

```powershell
terraform output
terraform plan
```

## IAM and add-on parity

- External Secrets Operator is installed by bootstrap and consumes the IRSA
  role created by this root.
- AWS Load Balancer Controller is not installed, so its optional IAM role is
  disabled.
- Velero is not installed. Its optional IAM role is disabled; the existing S3
  bucket remains a roadmap placeholder to avoid changing state ownership in
  this delivery.

## Capacity and cost budget

The DEV Helm render currently requests about `1.45` vCPU and `7.25 GiB` of
memory, including Kafka. Three `t3.medium` nodes provide `6` vCPU and `12 GiB`
of raw capacity. The difference is intentional headroom for EKS system pods,
Argo CD, External Secrets and a temporary surge pod during rolling updates.
Raw capacity is not the same as Kubernetes allocatable capacity.

For interview/demo planning, assume roughly **USD 7-10/day** or **USD
210-300/month** if DEV stays online continuously. This is a planning range, not
an invoice: region, data transfer, storage and current AWS prices change the
result. EKS standard support alone is USD 0.10 per cluster-hour; extended
support is substantially more expensive. NAT Gateway is disabled by default
because it adds hourly and data-processing charges. Verify the current Frankfurt
price in the [AWS Pricing Calculator](https://calculator.aws/) before creating
the environment.

Sources: [Amazon EKS pricing](https://aws.amazon.com/eks/pricing/) and
[Amazon VPC pricing](https://aws.amazon.com/vpc/pricing/).

Destroy DEV after every demo and run:

```bash
./scripts/aws/cost-control-check-dev.sh
```

## Cost Safety

NAT Gateway is disabled by default because it has hourly and data-processing costs.

Enable it only when private resources require outbound Internet access:

```hcl
enable_nat_gateway = true
```

## Static CI

`.github/workflows/terraform-ci.yml` runs `terraform fmt -check` and validates
the backend, shared and DEV roots with `terraform init -backend=false`. It has
read-only repository permissions, no AWS credentials and never runs plan,
apply or destroy. Infrastructure changes remain an explicit manual operation.

## Modules Used

This environment composes the reusable AWS modules under:

```text
platform/terraform/modules/aws
```
