# Create the private subnet group where ElastiCache can place Redis nodes.
resource "aws_elasticache_subnet_group" "main" {
  # Use a stable environment-specific name.
  name = "${local.name_prefix}-redis"

  # Keep Redis inside the existing private subnets.
  subnet_ids = var.private_subnet_ids

  # Apply standard project tags.
  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-redis"
      Role = "elasticache-subnet-group"
    }
  )

  lifecycle {
    precondition {
      # Keep two subnets available for future replicas or Multi-AZ.
      condition     = length(var.private_subnet_ids) >= 2
      error_message = "ElastiCache Redis requires at least two private subnets for this Atlas design."
    }
  }
}

# Allow Redis access only from the EKS cluster security group.
resource "aws_vpc_security_group_ingress_rule" "redis_from_eks" {
  # Add the rule to the security group attached to ElastiCache.
  security_group_id = var.cache_security_group_id

  # Allow only Redis TCP traffic.
  ip_protocol = "tcp"
  from_port   = var.port
  to_port     = var.port

  # Allow connections only from EKS-managed workloads.
  referenced_security_group_id = var.allowed_security_group_id

  # Keep the rule identifiable in AWS.
  description = "Allow Redis access from Atlas EKS managed nodes."
}