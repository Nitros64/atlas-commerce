# Create the shared Atlas ECR repositories.
module "ecr" {
  # Reference the local reusable ECR module.
  source = "../../../modules/aws/ecr"

  project     = var.project
  environment = var.environment

  # Build repository names such as atlas-commerce/auth-service.
  repository_prefix = var.repository_prefix
  repository_names  = var.repository_names

  # Configure repository security and cleanup behavior.
  scan_on_push                   = var.scan_on_push
  image_tag_mutability           = var.image_tag_mutability
  max_tagged_images              = var.max_tagged_images
  untagged_image_expiration_days = var.untagged_image_expiration_days

  # Reuse shared environment tags.
  additional_tags = var.additional_tags
}

# Create the GitHub Actions OIDC role used to push Atlas images to ECR.
module "github_actions_iam" {
  source = "../../../modules/aws/iam"

  project     = var.project
  environment = var.environment

  # Convert the ECR module output map into the list expected by the IAM module.
  ecr_repository_arns = values(module.ecr.repository_arns)

  additional_tags = var.additional_tags
}