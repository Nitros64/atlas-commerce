# Create secret containers only.
# Secret values are intentionally populated outside Terraform.
resource "aws_secretsmanager_secret" "this" {
  for_each = var.secret_names

  # Example: atlas-commerce/dev/platform
  name = "${var.project}/${var.environment}/${each.value}"

  description = "Runtime secret container for ${var.project} ${var.environment}: ${each.value}."

  # Keep a recovery period in case a secret is deleted accidentally.
  recovery_window_in_days = var.recovery_window_in_days

  # Uses the AWS-managed Secrets Manager key by default.
  tags = merge(
    local.common_tags,
    {
      Name   = "${local.name_prefix}-${replace(each.value, "/", "-")}"
      Secret = each.value
    }
  )
}