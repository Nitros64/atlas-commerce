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