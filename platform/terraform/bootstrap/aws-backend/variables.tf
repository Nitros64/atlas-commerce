variable "project" {
  description = "Project name used for naming AWS resources."
  type        = string
  default     = "atlas-commerce"
}

variable "environment" {
  description = "Environment name. For backend bootstrap we use shared."
  type        = string
  default     = "shared"
}

variable "aws_region" {
  description = "AWS region where the Terraform backend resources will be created."
  type        = string
  default     = "eu-central-1"
}

variable "state_bucket_name" {
  description = "Optional custom S3 bucket name for Terraform state. Must be globally unique."
  type        = string
  default     = null
}

variable "lock_table_name" {
  description = "Optional custom DynamoDB table name for Terraform state locking compatibility."
  type        = string
  default     = null
}

variable "force_destroy_state_bucket" {
  description = "Allows destroying the state bucket even if it contains objects. Keep false for safety."
  type        = bool
  default     = false
}

variable "enable_lock_table_deletion_protection" {
  description = "Enables deletion protection for the DynamoDB lock table."
  type        = bool
  default     = false
}