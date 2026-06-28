locals {
  # Build a consistent name prefix, for example atlas-commerce-dev.
  name_prefix = "${var.project}-${var.environment}"

  # Define common tags shared by all security groups.
  common_tags = merge(
    {
      Project     = var.project
      Environment = var.environment
      ManagedBy   = "terraform"
      Component   = "security-groups"
    },
    var.additional_tags
  )
}