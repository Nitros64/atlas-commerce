# Clean old images automatically to avoid unnecessary ECR storage costs.
resource "aws_ecr_lifecycle_policy" "this" {
  for_each = aws_ecr_repository.this

  repository = each.value.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Expire untagged images after the configured number of days"

        selection = {
          tagStatus   = "untagged"
          countType   = "sinceImagePushed"
          countUnit   = "days"
          countNumber = var.untagged_image_expiration_days
        }

        action = {
          type = "expire"
        }
      },
      {
        rulePriority = 2
        description  = "Keep only the configured number of SHA-tagged images"

        selection = {
          tagStatus     = "tagged"
          tagPrefixList = ["sha-"]
          countType     = "imageCountMoreThan"
          countNumber   = var.max_tagged_images
        }

        action = {
          type = "expire"
        }
      }
    ]
  })
}