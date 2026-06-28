# Create the Atlas development network by calling the reusable network module.
module "network" {
  # Reference the local reusable network module.
  source = "../../../modules/aws/network"

  # Pass project and environment values used for names and tags.
  project     = var.project
  environment = var.environment

  # Pass the VPC address range for development.
  vpc_cidr = var.vpc_cidr

  # Define how many Availability Zones the network should use.
  availability_zone_count = var.availability_zone_count

  # Pass the CIDR ranges for public subnets.
  public_subnet_cidrs = var.public_subnet_cidrs

  # Pass the CIDR ranges for private subnets.
  private_subnet_cidrs = var.private_subnet_cidrs

  # Keep NAT disabled by default to avoid hourly AWS costs in development.
  enable_nat_gateway = var.enable_nat_gateway

  # Pass any environment-specific tags to the network resources.
  additional_tags = var.additional_tags
}