data "aws_caller_identity" "velero_backup" {}

data "aws_region" "velero_backup" {}

# The bucket is retained as a state-compatible roadmap placeholder. DEV does
# not install Velero and the EKS module does not create its IAM role.
module "velero_backup" {
  source = "../../../modules/aws/velero-backup"

  project     = "atlas-commerce"
  environment = "dev"

  bucket_name = "atlas-commerce-dev-velero-${data.aws_caller_identity.velero_backup.account_id}-${data.aws_region.velero_backup.name}"
}
