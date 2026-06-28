resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr # Assign the private IP address range provided by the calling environment.
  enable_dns_support   = true         # Enable AWS internal DNS resolution inside the VPC.
  enable_dns_hostnames = true         # Allow AWS resources to receive DNS hostnames when applicable.

  tags = merge(        # Apply shared tags plus a human-readable VPC name.
    local.common_tags, # Reuse the standard project, environment, and Terraform tags.
    {
      Name = "${local.name_prefix}-vpc"
    }
  )
}

# Create an Internet Gateway and attach it to the VPC.
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id # Reference the VPC created above so Terraform creates the attachment.

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-igw"
    }
  )
}