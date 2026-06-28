# Reuse the S3 bucket created by the bootstrap phase.
bucket = "atlas-commerce-shared-tfstate-529601496188-eu-central-1"

# Keep development state isolated from bootstrap, staging, and production.
key = "atlas-commerce/dev/terraform.tfstate"

# Store the state in the same AWS region as the backend bucket.
region = "eu-central-1"

# Encrypt the Terraform state stored in S3.
encrypt = true

# Enable S3-native state locking.
use_lockfile = true