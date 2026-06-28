# Expose the VPC ID created by the network module.
output "vpc_id" {
  value = module.network.vpc_id
}

# Expose the public subnet IDs for future ALB or NAT resources.
output "public_subnet_ids" {
  value = module.network.public_subnet_ids
}

# Expose the private subnet IDs for future EKS nodes and data services.
output "private_subnet_ids" {
  value = module.network.private_subnet_ids
}

# Expose the Availability Zones selected for this environment.
output "availability_zones" {
  value = module.network.availability_zones
}

# Expose the NAT Gateway ID when NAT is enabled.
output "nat_gateway_id" {
  value = module.network.nat_gateway_id
}