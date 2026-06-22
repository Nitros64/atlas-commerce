# Atlas Commerce - Terraform Backend Bootstrap

This folder creates the initial AWS resources required for Terraform remote state.

Resources:

- S3 bucket for Terraform state
- S3 bucket versioning
- S3 bucket encryption
- S3 public access block
- S3 HTTPS-only bucket policy
- DynamoDB table for state locking compatibility

Important:

This bootstrap folder intentionally uses local Terraform state because it creates the remote backend itself.

## Usage

```bash
cd platform/terraform/bootstrap/aws-backend

cp terraform.tfvars.example terraform.tfvars

terraform init
terraform fmt -recursive
terraform validate
terraform plan -out tfplan
terraform apply tfplan