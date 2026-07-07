variable "project" {
  description = "Project name used for resource names and tags."
  type        = string
}

variable "environment" {
  description = "Environment name used for resource names and tags."
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnet IDs used by the RDS DB subnet group."
  type        = list(string)
}

variable "database_security_group_id" {
  description = "Security group attached to the RDS instance."
  type        = string
}

variable "allowed_security_group_id" {
  description = "Security group allowed to connect to PostgreSQL on port 5432."
  type        = string
}

variable "identifier" {
  description = "Stable RDS DB instance identifier."
  type        = string
}

variable "engine_version" {
  description = "Exact PostgreSQL engine version supported in the selected AWS Region."
  type        = string
}

variable "instance_class" {
  description = "RDS instance class."
  type        = string
  default     = "db.t4g.micro"
}

variable "allocated_storage_gib" {
  description = "Initial allocated gp3 storage in GiB."
  type        = number
  default     = 20
}

variable "master_username" {
  description = "Master username. RDS manages its password in Secrets Manager."
  type        = string
  default     = "atlas_admin"
}

variable "backup_retention_period_days" {
  description = "Number of days to retain automated backups."
  type        = number
  default     = 7

  validation {
    condition     = var.backup_retention_period_days >= 0 && var.backup_retention_period_days <= 35
    error_message = "backup_retention_period_days must be between 0 and 35."
  }
}

variable "multi_az" {
  description = "Whether to deploy a standby instance in another Availability Zone."
  type        = bool
  default     = false
}

variable "deletion_protection" {
  description = "Prevent accidental deletion of the DB instance."
  type        = bool
  default     = false
}

variable "skip_final_snapshot" {
  description = "Skip the final snapshot when destroying the DB instance."
  type        = bool
  default     = true
}

variable "additional_tags" {
  description = "Additional tags applied to RDS resources."
  type        = map(string)
  default     = {}
}