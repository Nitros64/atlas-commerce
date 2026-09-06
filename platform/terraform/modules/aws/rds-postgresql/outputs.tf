# Expose the private hostname used by applications inside the VPC.
output "address" {
  description = "Private DNS hostname of the PostgreSQL RDS instance."
  value       = aws_db_instance.main.address
}

# Expose the PostgreSQL listener port separately from the hostname.
output "port" {
  description = "PostgreSQL port exposed by the RDS instance."
  value       = aws_db_instance.main.port
}

# Expose the DB instance ARN for IAM, auditing, and future integrations.
output "arn" {
  description = "ARN of the PostgreSQL RDS instance."
  value       = aws_db_instance.main.arn
}

# Expose the ARN of the RDS-managed master credentials secret.
# The password itself is never exposed through Terraform outputs.
output "master_user_secret_arn" {
  description = "ARN of the Secrets Manager secret managed by RDS for the master user."
  value = try(
    aws_db_instance.main.master_user_secret[0].secret_arn,
    null
  )
}

# Expose the DB subnet group used by the instance.
output "db_subnet_group_name" {
  description = "Name of the DB subnet group used by PostgreSQL."
  value       = aws_db_subnet_group.main.name
}