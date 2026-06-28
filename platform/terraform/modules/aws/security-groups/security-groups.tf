
# reglas: Internet → ALB, ALB → EKS, EKS → RDS y EKS → Redis.


# Create the public-facing security group for the Application Load Balancer.
resource "aws_security_group" "alb" {
  # Use a stable and readable security group name.
  name = "${local.name_prefix}-alb-sg"

  # Describe the responsibility of this security group.
  description = "Controls inbound traffic to the Atlas Application Load Balancer."

  # Create the security group inside the target VPC.
  vpc_id = var.vpc_id

  tags = merge(
    local.common_tags,
    {
      # Show a readable name in the AWS console.
      Name = "${local.name_prefix}-alb-sg"

      # Identify the role of this security group.
      Role = "alb"
    }
  )
}

# Create the security group assigned to EKS worker nodes and workloads.
resource "aws_security_group" "eks_nodes" {
  # Use a stable and readable security group name.
  name = "${local.name_prefix}-eks-nodes-sg"

  # Describe the responsibility of this security group.
  description = "Controls traffic for Atlas EKS worker nodes and workloads."

  # Create the security group inside the target VPC.
  vpc_id = var.vpc_id

  tags = merge(
    local.common_tags,
    {
      # Show a readable name in the AWS console.
      Name = "${local.name_prefix}-eks-nodes-sg"

      # Identify the role of this security group.
      Role = "eks-nodes"
    }
  )
}

# Create the security group that will protect PostgreSQL databases.
resource "aws_security_group" "rds" {
  # Use a stable and readable security group name.
  name = "${local.name_prefix}-rds-sg"

  # Describe the responsibility of this security group.
  description = "Controls access to Atlas PostgreSQL databases."

  # Create the security group inside the target VPC.
  vpc_id = var.vpc_id

  tags = merge(
    local.common_tags,
    {
      # Show a readable name in the AWS console.
      Name = "${local.name_prefix}-rds-sg"

      # Identify the role of this security group.
      Role = "rds"
    }
  )
}

# Create the security group that will protect Redis clusters.
resource "aws_security_group" "redis" {
  # Use a stable and readable security group name.
  name = "${local.name_prefix}-redis-sg"

  # Describe the responsibility of this security group.
  description = "Controls access to Atlas Redis clusters."

  # Create the security group inside the target VPC.
  vpc_id = var.vpc_id

  tags = merge(
    local.common_tags,
    {
      # Show a readable name in the AWS console.
      Name = "${local.name_prefix}-redis-sg"

      # Identify the role of this security group.
      Role = "redis"
    }
  )
}