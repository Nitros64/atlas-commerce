# AWS Terraform Backend Bootstrap

Creates the remote backend used by Terraform:

- S3 bucket for state
- DynamoDB table for locking
- Encryption and public access protection

This is applied manually once before using the live environments.
