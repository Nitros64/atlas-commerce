# Create a cluster-mode-disabled Redis OSS replication group for Atlas.
resource "aws_elasticache_replication_group" "main" {
  # Use a stable identifier supplied by the live environment.
  replication_group_id = var.replication_group_id

  # Keep a readable description in AWS.
  description = var.description

  # Terraform uses "redis" as the Redis OSS engine identifier.
  engine         = "redis"
  engine_version = var.engine_version

  # Define the cache node size.
  node_type = var.node_type

  # Use the configured Redis listener port.
  port = var.port

  # One primary node in dev; future environments can add replicas.
  num_cache_clusters = var.num_cache_clusters

  # Enable failover only when replicas exist.
  automatic_failover_enabled = var.automatic_failover_enabled
  multi_az_enabled           = var.multi_az_enabled

  # Keep Redis private inside the Atlas VPC.
  subnet_group_name = aws_elasticache_subnet_group.main.name

  # Attach only the dedicated Redis security group.
  security_group_ids = [
    var.cache_security_group_id
  ]

  # Encrypt cached data at rest with the ElastiCache managed key.
  at_rest_encryption_enabled = var.at_rest_encryption_enabled

  # Require TLS between Atlas workloads and Redis.
  transit_encryption_enabled = var.transit_encryption_enabled
  transit_encryption_mode    = var.transit_encryption_enabled ? "required" : null

  # Optional Redis AUTH token.
  # Never place its value in Git or terraform.tfvars.
  auth_token = var.auth_token

  # Allow an optional custom parameter group later.
  parameter_group_name = var.parameter_group_name

  # Keep automatic snapshots according to the environment policy.
  snapshot_retention_limit = var.snapshot_retention_limit

  # Allow optional maintenance and snapshot windows.
  maintenance_window = var.preferred_maintenance_window
  snapshot_window    = var.preferred_snapshot_window

  # Apply development changes without waiting for a maintenance window.
  apply_immediately = var.apply_immediately

  # Allow compatible minor Redis OSS upgrades only.
  auto_minor_version_upgrade = true

  tags = merge(
    local.common_tags,
    {
      Name = var.replication_group_id
      Role = "redis"
    }
  )

  lifecycle {
    precondition {
      # Redis failover needs a primary plus at least one replica.
      condition = (
        !var.automatic_failover_enabled ||
        var.num_cache_clusters >= 2
      )

      error_message = "automatic_failover_enabled requires at least two cache nodes."
    }

    precondition {
      # Multi-AZ only makes sense with automatic failover and replicas.
      condition = (
        !var.multi_az_enabled ||
        (
          var.automatic_failover_enabled &&
          var.num_cache_clusters >= 2
        )
      )

      error_message = "multi_az_enabled requires automatic failover and at least two cache nodes."
    }

    precondition {
      # AWS requires TLS when using Redis AUTH.
      condition = (
        var.auth_token == null ||
        var.transit_encryption_enabled
      )

      error_message = "auth_token requires transit_encryption_enabled = true."
    }
  }
}