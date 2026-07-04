output "repository_urls" {
  description = "ECR repository URLs indexed by Atlas service name."

  value = {
    for service, repository in aws_ecr_repository.this :
    service => repository.repository_url
  }
}

output "repository_arns" {
  description = "ECR repository ARNs indexed by Atlas service name."

  value = {
    for service, repository in aws_ecr_repository.this :
    service => repository.arn
  }
}