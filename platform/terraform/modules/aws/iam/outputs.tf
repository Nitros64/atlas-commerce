# Expose the IAM role ARN used by GitHub Actions through OIDC.
output "github_actions_ecr_push_role_arn" {
  description = "ARN of the IAM role GitHub Actions assumes to push images to ECR."
  value       = aws_iam_role.github_actions_ecr_push.arn
}

# Expose the IAM role name for easier AWS CLI and console inspection.
output "github_actions_ecr_push_role_name" {
  description = "Name of the IAM role GitHub Actions assumes to push images to ECR."
  value       = aws_iam_role.github_actions_ecr_push.name
}

# Expose the GitHub OIDC provider ARN.
output "github_actions_oidc_provider_arn" {
  description = "ARN of the GitHub Actions OIDC provider registered in AWS."
  value       = aws_iam_openid_connect_provider.github_actions.arn
}