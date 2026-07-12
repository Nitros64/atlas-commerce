output "terraform_state_bucket_name" {
  description = "S3 bucket used for Terraform remote state."
  value       = aws_s3_bucket.terraform_state.bucket
}

output "terraform_state_bucket_arn" {
  description = "ARN of the S3 bucket used for Terraform remote state."
  value       = aws_s3_bucket.terraform_state.arn
}

output "aws_region" {
  description = "AWS region where the backend resources were created."
  value       = var.aws_region
}

output "backend_config_example_dev" {
  description = "Example backend configuration for live/aws/dev/backend.hcl."
  value       = <<EOT
bucket       = "${aws_s3_bucket.terraform_state.bucket}"
key          = "atlas-commerce/dev/terraform.tfstate"
region       = "${var.aws_region}"
encrypt      = true
use_lockfile = true
EOT
}