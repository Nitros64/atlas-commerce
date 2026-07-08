# Create the private subnet group where RDS can place its network interfaces.
resource "aws_db_subnet_group" "main" {
  # Use a stable environment-specific name.
  name = "${local.name_prefix}-postgresql"

  # RDS requires subnets spanning at least two Availability Zones.
  subnet_ids = var.private_subnet_ids

  # Apply standard project tags.
  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-postgresql"
      Role = "rds-db-subnet-group"
    }
  )

  lifecycle {
    precondition {
      # Fail early if the caller does not provide enough private subnets.
      condition     = length(var.private_subnet_ids) >= 2
      error_message = "RDS requires at least two private subnets in different Availability Zones."
    }
  }
}

# Allow PostgreSQL traffic only from EKS worker nodes through the cluster SG.
resource "aws_vpc_security_group_ingress_rule" "postgresql_from_eks" {
  # Add the rule to the security group attached to the RDS instance.
  security_group_id = var.database_security_group_id

  # Allow only PostgreSQL TCP traffic.
  ip_protocol = "tcp"
  from_port   = 5432
  to_port     = 5432

  # Allow connections only from the EKS cluster primary security group.
  referenced_security_group_id = var.allowed_security_group_id

  # Keep the rule identifiable in AWS.
  description = "Allow PostgreSQL access from Atlas EKS managed nodes."
}