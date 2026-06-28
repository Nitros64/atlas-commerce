# Reserve one static public IP address for the NAT Gateway.
resource "aws_eip" "nat" {
  # Create this Elastic IP only when NAT Gateway is enabled.
  count = var.enable_nat_gateway ? 1 : 0

  # Allocate the Elastic IP inside the VPC scope.
  domain = "vpc"

  tags = merge(
    local.common_tags,
    {
      # Example: atlas-commerce-dev-nat-eip.
      Name = "${local.name_prefix}-nat-eip"
    }
  )
}

# Create one NAT Gateway in the first public subnet.
resource "aws_nat_gateway" "main" {
  # Create this NAT Gateway only when outbound internet access is enabled.
  count = var.enable_nat_gateway ? 1 : 0

  # Attach the Elastic IP reserved above.
  allocation_id = aws_eip.nat[0].id

  # Place the NAT Gateway in a public subnet so it can reach the Internet Gateway.
  subnet_id = aws_subnet.public[0].id

  # Ensure the Internet Gateway exists before creating the NAT Gateway.
  depends_on = [aws_internet_gateway.main]

  tags = merge(
    local.common_tags,
    {
      # Example: atlas-commerce-dev-nat.
      Name = "${local.name_prefix}-nat"
    }
  )
}

# Add an outbound internet route to every private route table.
resource "aws_route" "private_nat_gateway" {
  # Create one route per private route table only when NAT is enabled.
  count = var.enable_nat_gateway ? var.availability_zone_count : 0

  # Add the route to the matching private route table.
  route_table_id = aws_route_table.private[count.index].id

  # Match all IPv4 traffic going outside the VPC.
  destination_cidr_block = "0.0.0.0/0"

  # Send that traffic through the shared NAT Gateway.
  nat_gateway_id = aws_nat_gateway.main[0].id
}