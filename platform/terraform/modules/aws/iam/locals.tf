locals {
  name_prefix = "${var.project}-${var.environment}"

  github_subject = "repo:${var.github_organization}/${var.github_repository}:ref:refs/heads/${var.github_branch}"

  common_tags = merge(
    {
      Project     = var.project
      Environment = var.environment
      ManagedBy   = "terraform"
      Component   = "iam"
    },
    var.additional_tags
  )
}