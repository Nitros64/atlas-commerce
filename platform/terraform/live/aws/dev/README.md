# Atlas Commerce - AWS Development Environment

Terraform root module for the Atlas Commerce development environment in AWS Frankfurt (`eu-central-1`).

## What This Environment Creates

* One VPC: `10.20.0.0/16`
* Two public subnets across `eu-central-1a` and `eu-central-1b`
* Two private subnets across `eu-central-1a` and `eu-central-1b`
* One Internet Gateway
* One public route table with Internet access
* One private route table per Availability Zone
* No NAT Gateway by default

## State Backend

Terraform state is stored remotely in the shared S3 backend:

```text
atlas-commerce/dev/terraform.tfstate
```

State locking uses S3 lockfiles.

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

## Cost Safety

NAT Gateway is disabled by default because it has hourly and data-processing costs.

Enable it only when private resources require outbound Internet access:

```hcl
enable_nat_gateway = true
```

## Module Used

This environment invokes the reusable network module:

```text
../../../modules/aws/network
```

The module is responsible for creating the VPC, subnets, route tables, Internet Gateway, and optional NAT Gateway.
