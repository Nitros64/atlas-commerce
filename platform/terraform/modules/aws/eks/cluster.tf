# Create the Amazon EKS control plane.
resource "aws_eks_cluster" "main" {
  # Use the explicit cluster name provided by the environment.
  name = var.cluster_name

  # Use the IAM role created for the EKS control plane.
  role_arn = aws_iam_role.cluster.arn

  # Pin the Kubernetes version chosen by the environment.
  version = var.kubernetes_version

  # Terraform manages VPC CNI, CoreDNS, and kube-proxy as EKS add-ons.
  bootstrap_self_managed_addons = false 

  # Enable only the control-plane logs requested by the environment.
  enabled_cluster_log_types = var.enabled_cluster_log_types

  vpc_config {
    # Place EKS control-plane ENIs in the private subnets.
    subnet_ids = var.private_subnet_ids

    # Attach extra control-plane security groups only when provided.
    security_group_ids = length(var.cluster_security_group_ids) > 0 ? var.cluster_security_group_ids : null

    # Allow nodes and in-VPC clients to reach the Kubernetes API privately.
    endpoint_private_access = var.cluster_endpoint_private_access

    # Control whether the Kubernetes API is reachable from outside the VPC.
    endpoint_public_access = var.cluster_endpoint_public_access

    # Restrict public API access to explicitly allowed CIDR ranges.
    public_access_cidrs = var.cluster_endpoint_public_access ? var.cluster_endpoint_public_access_cidrs : null
  }

  tags = merge(
    local.common_tags,
    {
      # Show a readable cluster name in the AWS console.
      Name = var.cluster_name

      # Identify this resource as the EKS control plane.
      Role = "eks-cluster"
    }
  )

  # Ensure the cluster IAM permissions are attached before EKS creates the control plane.
  depends_on = [
    aws_iam_role_policy_attachment.cluster_policy
  ]

  lifecycle {
    # EKS requires at least one API endpoint access mode.
    precondition {
      condition = (
        var.cluster_endpoint_private_access ||
        var.cluster_endpoint_public_access
      )

      error_message = "At least one EKS API endpoint access mode must be enabled."
    }

    # Never allow a public API endpoint without explicit CIDR restrictions.
    precondition {
      condition = (
        !var.cluster_endpoint_public_access ||
        length(var.cluster_endpoint_public_access_cidrs) > 0
      )

      error_message = "Public EKS API access requires at least one allowed CIDR."
    }
  }

  access_config {
    authentication_mode                         = "API_AND_CONFIG_MAP"
    bootstrap_cluster_creator_admin_permissions = true
  }

  upgrade_policy {
    support_type = "STANDARD"
  }
  
}