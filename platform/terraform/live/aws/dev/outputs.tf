# Expose the VPC ID created by the network module.
output "vpc_id" {
  value = module.network.vpc_id
}

# Expose the public subnet IDs for future ALB or NAT resources.
output "public_subnet_ids" {
  value = module.network.public_subnet_ids
}

# Expose the private subnet IDs for future EKS nodes and data services.
output "private_subnet_ids" {
  value = module.network.private_subnet_ids
}

# Expose the Availability Zones selected for this environment.
output "availability_zones" {
  value = module.network.availability_zones
}

# Expose the NAT Gateway ID when NAT is enabled.
output "nat_gateway_id" {
  value = module.network.nat_gateway_id
}

# Expose the future ALB security group ID.
output "alb_security_group_id" {
  value = module.security_groups.alb_security_group_id
}

# Expose the future EKS nodes security group ID.
output "eks_nodes_security_group_id" {
  value = module.security_groups.eks_nodes_security_group_id
}

# Expose the PostgreSQL security group ID.
output "rds_security_group_id" {
  value = module.security_groups.rds_security_group_id
}

# Expose the Redis security group ID.
output "redis_security_group_id" {
  value = module.security_groups.redis_security_group_id
}

output "platform_secret_arn" {
  description = "ARN of the Atlas development platform secret."
  value       = module.secrets_manager.secret_arns["platform"]
}

output "platform_secret_name" {
  description = "Name of the Atlas development platform secret."
  value       = module.secrets_manager.secret_names["platform"]
}

output "rds_postgresql_address" {
  description = "Private DNS hostname of the Atlas development PostgreSQL RDS instance."
  value       = module.rds_postgresql.address
}

output "rds_postgresql_port" {
  description = "PostgreSQL listener port."
  value       = module.rds_postgresql.port
}

output "rds_postgresql_master_secret_arn" {
  description = "ARN of the RDS-managed master credentials secret."
  value       = module.rds_postgresql.master_user_secret_arn
}