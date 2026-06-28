locals {
  # Build a consistent resource name prefix, for example atlas-commerce-dev.
  name_prefix = "${var.project}-${var.environment}"

  # Define tags automatically applied to Atlas development resources.
  common_tags = merge(
    {
      Project     = var.project
      Environment = var.environment
      ManagedBy   = "terraform"
      Component   = "platform"
    },
    var.additional_tags
  )
}