# Define the minimum Terraform CLI version required by this root module.
terraform {
  required_version = ">= 1.10.0"

  # Declare the AWS provider and its compatible version range.
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}