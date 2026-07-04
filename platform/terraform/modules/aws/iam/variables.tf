variable "project" {
  type = string
}

variable "environment" {
  type = string
}

variable "github_organization" {
  description = "GitHub organization or user that owns the repository."
  type        = string
  default     = "Nitros64"
}

variable "github_repository" {
  description = "GitHub repository allowed to assume the AWS role."
  type        = string
  default     = "atlas-commerce"
}

variable "github_branch" {
  description = "Git branch allowed to publish images to ECR."
  type        = string
  default     = "master"
}

variable "ecr_repository_arns" {
  description = "ECR repository ARNs that GitHub Actions can push images to."
  type        = list(string)
}

variable "role_name" {
  description = "IAM role name assumed by GitHub Actions through OIDC."
  type        = string
  default     = "atlas-commerce-github-actions-ecr-push-role"
}

variable "additional_tags" {
  type    = map(string)
  default = {}
}