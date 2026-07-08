output "secret_arns" {
  description = "ARNs of Secrets Manager secret containers."
  value = {
    for name, secret in aws_secretsmanager_secret.this : name => secret.arn
  }
}

output "secret_names" {
  description = "Names of Secrets Manager secret containers."
  value = {
    for name, secret in aws_secretsmanager_secret.this : name => secret.name
  }
}