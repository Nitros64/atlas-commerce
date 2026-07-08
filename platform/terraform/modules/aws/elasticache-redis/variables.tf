variable "project" {
  description = "Project name used for resource names and tags."
  type        = string
}

variable "environment" {
  description = "Environment name used for resource names and tags."
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnet IDs used by the ElastiCache subnet group."
  type        = list(string)
}

variable "cache_security_group_id" {
  description = "Security group attached to the ElastiCache replication group."
  type        = string
}

variable "allowed_security_group_id" {
  description = "Security group allowed to connect to Redis."
  type        = string
}

variable "replication_group_id" {
  description = "Stable identifier for the ElastiCache replication group."
  type        = string
}

variable "description" {
  description = "Human-readable description of the Redis replication group."
  type        = string
  default     = "Atlas Commerce managed Redis cache."
}

variable "engine_version" {
  description = "Exact Redis OSS engine version available in the selected AWS Region."
  type        = string
}

variable "node_type" {
  description = "ElastiCache node type used by the replication group."
  type        = string
}

variable "port" {
  description = "Redis listener port."
  type        = number
  default     = 6379

  validation {
    condition     = var.port > 0 && var.port <= 65535
    error_message = "port must be between 1 and 65535."
  }
}

variable "num_cache_clusters" {
  description = "Total nodes in the cluster-mode-disabled replication group, including the primary node."
  type        = number
  default     = 1

  validation {
    condition     = var.num_cache_clusters >= 1 && var.num_cache_clusters <= 6
    error_message = "num_cache_clusters must be between 1 and 6."
  }
}

variable "automatic_failover_enabled" {
  description = "Enable automatic failover between a primary node and replicas."
  type        = bool
  default     = false
}

variable "multi_az_enabled" {
  description = "Place primary and replica nodes across Availability Zones."
  type        = bool
  default     = false
}

variable "at_rest_encryption_enabled" {
  description = "Encrypt Redis data at rest."
  type        = bool
  default     = true
}

variable "transit_encryption_enabled" {
  description = "Encrypt Redis traffic in transit using TLS."
  type        = bool
  default     = true
}

variable "auth_token" {
  description = "Redis AUTH token. Supply it outside Git and terraform.tfvars."
  type        = string
  default     = null
  nullable    = true
  sensitive   = true
}

variable "parameter_group_name" {
  description = "Optional ElastiCache parameter group name."
  type        = string
  default     = null
  nullable    = true
}

variable "snapshot_retention_limit" {
  description = "Number of days to retain automatic Redis snapshots."
  type        = number
  default     = 7

  validation {
    condition     = var.snapshot_retention_limit >= 0 && var.snapshot_retention_limit <= 35
    error_message = "snapshot_retention_limit must be between 0 and 35."
  }
}

variable "preferred_maintenance_window" {
  description = "Optional UTC maintenance window."
  type        = string
  default     = null
  nullable    = true
}

variable "preferred_snapshot_window" {
  description = "Optional UTC snapshot window."
  type        = string
  default     = null
  nullable    = true
}

variable "apply_immediately" {
  description = "Apply Redis configuration changes immediately in development."
  type        = bool
  default     = true
}

variable "additional_tags" {
  description = "Additional tags applied to ElastiCache resources."
  type        = map(string)
  default     = {}
}