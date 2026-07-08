# Read the current AWS account, region, and partition dynamically.
data "aws_caller_identity" "current" {}

data "aws_region" "current" {}

data "aws_partition" "current" {}

locals {
  # Pin the namespace and ServiceAccount that Helm will create later.
  external_secrets_namespace            = "external-secrets"
  external_secrets_service_account_name = "external-secrets"

  # Restrict ESO to Atlas secrets for this environment only.
  external_secrets_secret_arn_pattern = "arn:${data.aws_partition.current.partition}:secretsmanager:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:secret:${var.project}/${var.environment}/*"
}

# Build the IRSA trust policy for External Secrets Operator.
data "aws_iam_policy_document" "external_secrets_assume_role" {
  count = var.enable_irsa ? 1 : 0

  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.eks[0].arn]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.oidc_issuer_hostpath}:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.oidc_issuer_hostpath}:sub"
      values = [
        "system:serviceaccount:${local.external_secrets_namespace}:${local.external_secrets_service_account_name}"
      ]
    }
  }
}

# Create the dedicated IAM role assumed only by External Secrets Operator.
resource "aws_iam_role" "external_secrets" {
  count = var.enable_irsa ? 1 : 0

  name = "${local.name_prefix}-external-secrets-role"

  assume_role_policy = data.aws_iam_policy_document.external_secrets_assume_role[0].json

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-external-secrets-role"
      Role = "external-secrets"
    }
  )
}

# Allow ESO to read only Atlas secrets belonging to this environment.
data "aws_iam_policy_document" "external_secrets_read" {
  count = var.enable_irsa ? 1 : 0

  statement {
    sid    = "ReadAtlasEnvironmentSecrets"
    effect = "Allow"

    actions = [
      "secretsmanager:DescribeSecret",
      "secretsmanager:GetSecretValue",
    ]

    resources = [
      local.external_secrets_secret_arn_pattern
    ]
  }
}

# Attach the scoped read policy to the ESO role.
resource "aws_iam_role_policy" "external_secrets_read" {
  count = var.enable_irsa ? 1 : 0

  name = "${local.name_prefix}-external-secrets-read"

  role   = aws_iam_role.external_secrets[0].id
  policy = data.aws_iam_policy_document.external_secrets_read[0].json
}