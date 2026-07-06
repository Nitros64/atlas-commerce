# Convert the default VPC CNI installation into an AWS-managed EKS add-on.
resource "aws_eks_addon" "vpc_cni" {
  # Install the add-on in the Atlas EKS cluster.
  cluster_name = aws_eks_cluster.main.name

  # Use the official Amazon VPC CNI add-on.
  addon_name = "vpc-cni"

  # Replace the default self-managed configuration during migration.
  resolve_conflicts_on_create = "OVERWRITE"

  # Keep future custom configuration when updating the add-on.
  resolve_conflicts_on_update = "PRESERVE"

  # Assign the dedicated IRSA role to the aws-node service account.
  service_account_role_arn = var.enable_irsa ? aws_iam_role.vpc_cni[0].arn : null

  # Wait until worker nodes and CNI permissions exist.
  # depends_on = [
  #   aws_eks_node_group.default,
  #   aws_iam_role_policy_attachment.node_cni_policy
  # ]

  depends_on = [
    aws_iam_role_policy_attachment.vpc_cni_policy
  ]
}

# Convert the default CoreDNS installation into an AWS-managed EKS add-on.
resource "aws_eks_addon" "coredns" {
  # Install the add-on in the Atlas EKS cluster.
  cluster_name = aws_eks_cluster.main.name

  # Use the official CoreDNS add-on.
  addon_name = "coredns"

  # Replace the default self-managed configuration during migration.
  resolve_conflicts_on_create = "OVERWRITE"

  # Keep future custom configuration when updating the add-on.
  resolve_conflicts_on_update = "PRESERVE"

  # CoreDNS needs worker nodes to schedule its Pods.
  depends_on = [
    aws_eks_node_group.default
  ]
}

# Convert the default kube-proxy installation into an AWS-managed EKS add-on.
resource "aws_eks_addon" "kube_proxy" {
  # Install the add-on in the Atlas EKS cluster.
  cluster_name = aws_eks_cluster.main.name

  # Use the official kube-proxy add-on.
  addon_name = "kube-proxy"

  # Replace the default self-managed configuration during migration.
  resolve_conflicts_on_create = "OVERWRITE"

  # Keep future custom configuration when updating the add-on.
  resolve_conflicts_on_update = "PRESERVE"

  # kube-proxy runs on worker nodes.
  depends_on = [
    aws_eks_node_group.default
  ]
}

# Install the Amazon EBS CSI driver as an AWS-managed EKS add-on.
resource "aws_eks_addon" "ebs_csi" {
  count = var.enable_irsa ? 1 : 0

  cluster_name = aws_eks_cluster.main.name
  addon_name   = "aws-ebs-csi-driver"

  resolve_conflicts_on_create = "OVERWRITE"
  resolve_conflicts_on_update = "PRESERVE"

  service_account_role_arn = aws_iam_role.ebs_csi[0].arn

  depends_on = [
    aws_eks_node_group.default,
    aws_iam_role_policy_attachment.ebs_csi_policy,
  ]
}