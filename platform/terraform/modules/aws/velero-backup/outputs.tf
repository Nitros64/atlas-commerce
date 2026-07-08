output "bucket_name" {
  description = "Velero backup S3 bucket name."
  value       = aws_s3_bucket.main.bucket
}

output "bucket_arn" {
  description = "Velero backup S3 bucket ARN."
  value       = aws_s3_bucket.main.arn
}