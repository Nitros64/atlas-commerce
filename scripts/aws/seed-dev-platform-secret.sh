#!/usr/bin/env bash
set -euo pipefail

REGION="eu-central-1"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
TERRAFORM_DIR="$PROJECT_ROOT/platform/terraform/live/aws/dev"

PLATFORM_SECRET_NAME="$(terraform -chdir="$TERRAFORM_DIR" output -raw platform_secret_name)"

JWT_SECRET="$(openssl rand -base64 48)"
REDIS_PASSWORD="$(openssl rand -base64 32)"
POSTGRES_PASSWORD="$(openssl rand -base64 32)"

SECRET_STRING=$(cat <<EOF
{
  "SECURITY_JWT_SECRET": "$JWT_SECRET",
  "REDIS_PASSWORD": "$REDIS_PASSWORD",
  "POSTGRES_PASSWORD": "$POSTGRES_PASSWORD"
}
EOF
)

aws secretsmanager put-secret-value \
  --region "$REGION" \
  --secret-id "$PLATFORM_SECRET_NAME" \
  --secret-string "$SECRET_STRING"

unset JWT_SECRET
unset REDIS_PASSWORD
unset POSTGRES_PASSWORD
unset SECRET_STRING

echo "Seeded platform secret: $PLATFORM_SECRET_NAME"
echo "Keys:"
echo "- SECURITY_JWT_SECRET"
echo "- REDIS_PASSWORD"
echo "- POSTGRES_PASSWORD"