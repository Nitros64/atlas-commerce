locals {
  # Build a consistent prefix, for example atlas-commerce-dev.
  name_prefix = "${var.project}-${var.environment}"

  # Define tags shared by EKS resources created by this module.
  common_tags = merge(
    {
      Project     = var.project
      Environment = var.environment
      ManagedBy   = "terraform"
      Component   = "eks"
    },
    var.additional_tags
  )
}