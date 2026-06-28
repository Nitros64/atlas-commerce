# Expose the public ALB security group ID.
output "alb_security_group_id" {
  value = aws_security_group.alb.id
}

# Expose the EKS nodes security group ID.
output "eks_nodes_security_group_id" {
  value = aws_security_group.eks_nodes.id
}

# Expose the PostgreSQL security group ID.
output "rds_security_group_id" {
  value = aws_security_group.rds.id
}

# Expose the Redis security group ID.
output "redis_security_group_id" {
  value = aws_security_group.redis.id
}