# Build the IRSA trust policy for the AWS Load Balancer Controller service account.
data "aws_iam_policy_document" "aws_load_balancer_controller_assume_role" {
  # Create this policy only when IRSA is enabled.
  count = var.enable_irsa ? 1 : 0

  statement {
    # Allow the Kubernetes service account to request temporary AWS credentials.
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      # Trust the OIDC provider created for this EKS cluster.
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.eks[0].arn]
    }

    condition {
      # Require the AWS STS audience.
      test     = "StringEquals"
      variable = "${local.oidc_issuer_hostpath}:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      # Restrict this role to the controller service account only.
      test     = "StringEquals"
      variable = "${local.oidc_issuer_hostpath}:sub"
      values = [
        "system:serviceaccount:kube-system:aws-load-balancer-controller"
      ]
    }
  }
}

# Store the official IAM policy pinned to controller version 3.4.0.
resource "aws_iam_policy" "aws_load_balancer_controller" {
  # Create the policy only when IRSA is enabled.
  count = var.enable_irsa ? 1 : 0

  # Keep the policy name environment-specific.
  name = "${local.name_prefix}-aws-load-balancer-controller"

  # Record the controller version matched by this policy.
  description = "IAM permissions for AWS Load Balancer Controller v3.4.0."

  # Read the reviewed policy committed inside this Terraform module.
  policy = file(
    "${path.module}/policies/aws-load-balancer-controller-v3.4.0.json"
  )

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-aws-load-balancer-controller"
      Role = "aws-load-balancer-controller-policy"
    }
  )
}

# Create the IAM role assumed only by the controller service account.
resource "aws_iam_role" "aws_load_balancer_controller" {
  # Create the role only when IRSA is enabled.
  count = var.enable_irsa ? 1 : 0

  # Use a stable, readable IAM role name.
  name = "${local.name_prefix}-aws-load-balancer-controller-role"

  # Apply the trust policy defined above.
  assume_role_policy = data.aws_iam_policy_document.aws_load_balancer_controller_assume_role[0].json

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-aws-load-balancer-controller-role"
      Role = "aws-load-balancer-controller"
    }
  )
}

# Attach the controller permissions to its dedicated IRSA role.
resource "aws_iam_role_policy_attachment" "aws_load_balancer_controller" {
  # Create this attachment only when IRSA is enabled.
  count = var.enable_irsa ? 1 : 0

  # Attach the policy to the dedicated controller role.
  role = aws_iam_role.aws_load_balancer_controller[0].name

  # Use the policy pinned to controller version 3.4.0.
  policy_arn = aws_iam_policy.aws_load_balancer_controller[0].arn
}