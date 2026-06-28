# Expose the VPC ID to the calling environment.
output "vpc_id" {
  # Return the ID of the Atlas VPC.
  value = aws_vpc.main.id
}

# Expose the CIDR block assigned to the VPC.
output "vpc_cidr_block" {
  # Return the CIDR received from the calling environment.
  value = aws_vpc.main.cidr_block
}

# Expose the Availability Zones selected by this module.
output "availability_zones" {
  # Return the AZs used for the public and private subnets.
  value = local.availability_zones
}

# Expose all public subnet IDs.
output "public_subnet_ids" {
  # Return the IDs of the public subnets created with count.
  value = aws_subnet.public[*].id
}

# Expose all private subnet IDs.
output "private_subnet_ids" {
  # Return the IDs of the private subnets created with count.
  value = aws_subnet.private[*].id
}

# Expose the Internet Gateway ID.
output "internet_gateway_id" {
  # Return the Internet Gateway attached to the VPC.
  value = aws_internet_gateway.main.id
}

# Expose the route table used by public subnets.
output "public_route_table_id" {
  # Return the shared public route table ID.
  value = aws_route_table.public.id
}

# Expose the route tables used by private subnets.
output "private_route_table_ids" {
  # Return one private route table ID per Availability Zone.
  value = aws_route_table.private[*].id
}

# Expose the NAT Gateway ID only when NAT is enabled.
output "nat_gateway_id" {
  # Return null instead of failing when no NAT Gateway exists.
  value = try(aws_nat_gateway.main[0].id, null)
}