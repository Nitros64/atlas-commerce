# Create the default managed worker node group for Atlas.
resource "aws_eks_node_group" "default" {
  # Attach this node group to the EKS control plane created in cluster.tf.
  cluster_name = aws_eks_cluster.main.name

  # Use the node group name provided by the environment.
  node_group_name = var.node_group_name

  # Use the IAM role assumed by the EC2 worker nodes.
  node_role_arn = aws_iam_role.node.arn

  # Launch worker nodes only in private subnets.
  subnet_ids = var.private_subnet_ids

  # Use the EKS-optimized AMI type selected by the environment.
  ami_type = var.node_ami_type

  # Choose On-Demand or Spot capacity.
  capacity_type = var.node_capacity_type

  # Allow the environment to select compatible EC2 instance types.
  instance_types = var.node_instance_types

  # Set the root EBS disk size for every worker node.
  disk_size = var.node_disk_size_gib

  scaling_config {
    # Keep at least this number of nodes available.
    min_size = var.node_min_size

    # Create this number of nodes initially.
    desired_size = var.node_desired_size

    # Allow the autoscaler to grow up to this limit later.
    max_size = var.node_max_size
  }

  update_config {
    # Replace only one node at a time during managed upgrades.
    max_unavailable = 1
  }

  tags = merge(
    local.common_tags,
    {
      # Show a readable node group name in AWS.
      Name = "${local.name_prefix}-${var.node_group_name}-nodes"

      # Identify this resource as the default worker group.
      Role = "eks-node-group"
    }
  )

  # Ensure required node IAM permissions exist before node creation.
  depends_on = [
    aws_iam_role_policy_attachment.node_worker_policy,
    aws_iam_role_policy_attachment.node_ecr_pull_policy,
    aws_eks_addon.vpc_cni
  ]

  lifecycle {
    # Prevent invalid node scaling values.
    precondition {
      condition = (
        var.node_min_size <= var.node_desired_size &&
        var.node_desired_size <= var.node_max_size
      )

      error_message = "node_min_size must be <= node_desired_size, which must be <= node_max_size."
    }
  }
}