# Register GitHub Actions as a trusted OIDC identity provider in this AWS account.
resource "aws_iam_openid_connect_provider" "github_actions" {
  # GitHub publishes OIDC tokens from this issuer URL.
  url = "https://token.actions.githubusercontent.com"

  # The official AWS GitHub Action requests tokens for AWS STS.
  client_id_list = ["sts.amazonaws.com"]

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-github-actions-oidc"
      Role = "github-actions-oidc-provider"
    }
  )
}

# Build the trust policy that limits role assumption to this repository and branch.
data "aws_iam_policy_document" "github_actions_assume_role" {
  statement {
    # Allow GitHub OIDC tokens to request temporary AWS credentials.
    effect = "Allow"

    actions = [
      "sts:AssumeRoleWithWebIdentity"
    ]

    principals {
      # Trust only the GitHub OIDC provider created above.
      type = "Federated"

      identifiers = [
        aws_iam_openid_connect_provider.github_actions.arn
      ]
    }

    condition {
      # Require the audience expected by AWS STS.
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      # Restrict access to Nitros64/atlas-commerce on master only.
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = [local.github_subject]
    }
  }
}

# Create the IAM role assumed by GitHub Actions through OIDC.
resource "aws_iam_role" "github_actions_ecr_push" {
  # Use the configured role name.
  name = var.role_name

  # Attach the GitHub OIDC trust policy.
  assume_role_policy = data.aws_iam_policy_document.github_actions_assume_role.json

  # Limit temporary workflow sessions to one hour.
  max_session_duration = 3600

  tags = merge(
    local.common_tags,
    {
      Name = var.role_name
      Role = "github-actions-ecr-push"
    }
  )
}

# Build the least-privilege permissions policy for publishing images to ECR.
data "aws_iam_policy_document" "github_actions_ecr_push" {
  statement {
    sid = "EcrAuthorizationToken"

    effect = "Allow"

    # ECR authentication tokens cannot be scoped to individual repositories.
    actions = [
      "ecr:GetAuthorizationToken"
    ]

    resources = ["*"]
  }

  statement {
    sid = "PushImagesToAtlasRepositories"

    effect = "Allow"

    # Allow only the API calls required to upload image layers and manifests.
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:CompleteLayerUpload",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart",
      "ecr:BatchGetImage"
    ]

    # Restrict image pushes to the ECR repositories passed by live/aws/shared.
    resources = var.ecr_repository_arns
  }
}

# Attach the ECR push policy to the GitHub Actions role.
resource "aws_iam_role_policy" "github_actions_ecr_push" {
  # Use a readable inline policy name.
  name = "${local.name_prefix}-github-actions-ecr-push"

  # Attach the policy to the OIDC-assumed role.
  role = aws_iam_role.github_actions_ecr_push.id

  # Use the least-privilege ECR policy built above.
  policy = data.aws_iam_policy_document.github_actions_ecr_push.json
}