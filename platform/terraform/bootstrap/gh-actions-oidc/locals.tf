locals {
  name_prefix = "gh-actions-${var.project}"

  common_tags = {
    Project   = var.project
    ManagedBy = "terraform"
    Component = "gh-actions-oidc"
  }
}
