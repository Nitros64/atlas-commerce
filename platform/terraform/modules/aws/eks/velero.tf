locals {
  velero_service_account_subject = "system:serviceaccount:${var.velero_namespace}:${var.velero_service_account_name}"
}

data "aws_iam_policy_document" "velero_assume_role" {
  count = var.enable_velero_irsa ? 1 : 0

  statement {
    effect = "Allow"

    actions = [
      "sts:AssumeRoleWithWebIdentity"
    ]

    principals {
      type = "Federated"

      identifiers = [
        aws_iam_openid_connect_provider.eks[0].arn
      ]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.oidc_issuer_hostpath}:aud"

      values = [
        "sts.amazonaws.com"
      ]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.oidc_issuer_hostpath}:sub"

      values = [
        local.velero_service_account_subject
      ]
    }
  }
}

resource "aws_iam_role" "velero" {
  count = var.enable_velero_irsa ? 1 : 0

  name = "${local.name_prefix}-velero-irsa-role"

  assume_role_policy = data.aws_iam_policy_document.velero_assume_role[0].json

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-velero-irsa-role"
    }
  )
}

data "aws_iam_policy_document" "velero" {
  count = var.enable_velero_irsa ? 1 : 0

  statement {
    sid    = "AllowEbsSnapshotOperations"
    effect = "Allow"

    actions = [
      "ec2:DescribeVolumes",
      "ec2:DescribeSnapshots",
      "ec2:CreateTags",
      "ec2:CreateVolume",
      "ec2:CreateSnapshot",
      "ec2:DeleteSnapshot"
    ]

    resources = [
      "*"
    ]
  }

  statement {
    sid    = "AllowVeleroBackupObjectAccess"
    effect = "Allow"

    actions = [
      "s3:GetObject",
      "s3:DeleteObject",
      "s3:PutObject",
      "s3:PutObjectTagging",
      "s3:AbortMultipartUpload",
      "s3:ListMultipartUploadParts"
    ]

    resources = [
      "${var.velero_backup_bucket_arn}/*"
    ]
  }

  statement {
    sid    = "AllowVeleroBackupBucketAccess"
    effect = "Allow"

    actions = [
      "s3:ListBucket"
    ]

    resources = [
      var.velero_backup_bucket_arn
    ]
  }
}

resource "aws_iam_policy" "velero" {
  count = var.enable_velero_irsa ? 1 : 0

  name        = "${local.name_prefix}-velero-policy"
  description = "Allows Velero to store backups in S3 and manage EBS snapshots."

  policy = data.aws_iam_policy_document.velero[0].json

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-velero-policy"
    }
  )
}

resource "aws_iam_role_policy_attachment" "velero" {
  count = var.enable_velero_irsa ? 1 : 0

  role       = aws_iam_role.velero[0].name
  policy_arn = aws_iam_policy.velero[0].arn
}