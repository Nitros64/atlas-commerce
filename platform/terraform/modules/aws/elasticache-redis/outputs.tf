# Expose the primary endpoint used by Atlas services for read/write Redis traffic.
output "primary_endpoint_address" {
  description = "Primary Redis endpoint hostname for read/write connections."
  value       = aws_elasticache_replication_group.main.primary_endpoint_address
}

# Expose the reader endpoint for future read-only workloads or replicas.
output "reader_endpoint_address" {
  description = "Reader Redis endpoint hostname. It may be empty when no replicas exist."
  value = try(
    aws_elasticache_replication_group.main.reader_endpoint_address,
    null
  )
}

# Expose the Redis listener port.
output "port" {
  description = "Redis listener port."
  value       = aws_elasticache_replication_group.main.port
}

# Expose the replication group ARN for auditing and future IAM integrations.
output "arn" {
  description = "ARN of the ElastiCache replication group."
  value       = aws_elasticache_replication_group.main.arn
}

# Expose the subnet group used by Redis.
output "subnet_group_name" {
  description = "Name of the ElastiCache subnet group used by Redis."
  value       = aws_elasticache_subnet_group.main.name
}