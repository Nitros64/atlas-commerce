variable "project" {
  description = "Project name used for resource naming and tags."
  type        = string
}

variable "environment" {
  description = "Deployment environment."
  type        = string
}

variable "bucket_name" {
  description = "Globally unique S3 bucket name for Velero backups."
  type        = string
}

variable "noncurrent_version_expiration_days" {
  description = "Days to retain noncurrent S3 object versions."
  type        = number
  default     = 30

  validation {
    condition     = var.noncurrent_version_expiration_days >= 7
    error_message = "Noncurrent object versions must be retained for at least 7 days."
  }
}

variable "tags" {
  description = "Additional tags for backup resources."
  type        = map(string)
  default     = {}
}