# Expose repository URLs for GitHub Actions and Helm values.
output "ecr_repository_urls" {
  value = module.ecr.repository_urls
}

# Expose repository ARNs for future IAM policies.
output "ecr_repository_arns" {
  value = module.ecr.repository_arns
}

output "github_actions_ecr_push_role_arn" {
  value = module.github_actions_iam.github_actions_ecr_push_role_arn
}

output "github_actions_ecr_push_role_name" {
  value = module.github_actions_iam.github_actions_ecr_push_role_name
}

output "github_actions_oidc_provider_arn" {
  value = module.github_actions_iam.github_actions_oidc_provider_arn
}