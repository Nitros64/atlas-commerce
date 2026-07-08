# Expose the EKS cluster name to the calling environment.
output "cluster_name" {
  value = aws_eks_cluster.main.name
}

# Expose the EKS cluster ARN.
output "cluster_arn" {
  value = aws_eks_cluster.main.arn
}

# Expose the Kubernetes API endpoint for kubeconfig and providers.
output "cluster_endpoint" {
  value = aws_eks_cluster.main.endpoint
}

# Expose the base64-encoded cluster CA data for Kubernetes clients.
output "cluster_certificate_authority_data" {
  value = aws_eks_cluster.main.certificate_authority[0].data
}

# Expose the Kubernetes version running on the control plane.
output "kubernetes_version" {
  value = aws_eks_cluster.main.version
}

# Expose the EKS-managed cluster security group ID.
output "cluster_primary_security_group_id" {
  value = aws_eks_cluster.main.vpc_config[0].cluster_security_group_id
}

# Expose the OIDC issuer URL used by IRSA.
output "oidc_issuer_url" {
  value = aws_eks_cluster.main.identity[0].oidc[0].issuer
}

# Expose the IAM OIDC provider ARN when IRSA is enabled.
output "oidc_provider_arn" {
  value = try(aws_iam_openid_connect_provider.eks[0].arn, null)
}

# Expose the dedicated VPC CNI IAM role ARN when IRSA is enabled.
output "vpc_cni_role_arn" {
  value = try(aws_iam_role.vpc_cni[0].arn, null)
}

# Expose the managed node group name.
output "node_group_name" {
  value = aws_eks_node_group.default.node_group_name
}

# Expose the managed node group ARN.
output "node_group_arn" {
  value = aws_eks_node_group.default.arn
}

# Expose the IAM role assigned to the EBS CSI controller.
output "ebs_csi_role_arn" {
  description = "ARN of the IAM role used by the Amazon EBS CSI controller."
  value       = try(aws_iam_role.ebs_csi[0].arn, null)
}

output "aws_load_balancer_controller_role_arn" {
  description = "ARN of the IAM role used by AWS Load Balancer Controller."
  value       = try(aws_iam_role.aws_load_balancer_controller[0].arn, null)
}

output "external_secrets_role_arn" {
  description = "ARN of the IAM role used by External Secrets Operator."
  value       = try(aws_iam_role.external_secrets[0].arn, null)
}

output "velero_role_arn" {
  description = "IAM role ARN used by Velero through IRSA."
  value       = try(aws_iam_role.velero[0].arn, null)
}