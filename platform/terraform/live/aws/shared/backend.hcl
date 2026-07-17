# Reuse the S3 bucket created by the bootstrap phase.
bucket = "atlas-commerce-shared-tfstate-724772086459-eu-central-1"

# Keep shared state isolated from bootstrap, dev, staging, and production.
key = "atlas-commerce/shared/terraform.tfstate"

# Store the state in the same AWS region as the backend bucket.
region = "eu-central-1"

# Encrypt the Terraform state stored in S3.
encrypt = true

# Enable S3-native state locking.
use_lockfile = true
