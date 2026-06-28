# Create one route table shared by all public subnets.
resource "aws_route_table" "public" {
  # Attach this route table to the Atlas VPC.
  vpc_id = aws_vpc.main.id

  # Send all IPv4 internet-bound traffic through the Internet Gateway.
  route {
    cidr_block = "0.0.0.0/0"

    # Reference the Internet Gateway created in vpc.tf.
    gateway_id = aws_internet_gateway.main.id
  }

  tags = merge(
    local.common_tags,
    {
      # Example: atlas-commerce-dev-public-rt.
      Name = "${local.name_prefix}-public-rt"

      # Identify this route table as public.
      Tier = "public"
    }
  )
}

# Associate every public subnet with the public route table.
resource "aws_route_table_association" "public" {
  # Create one association for each selected Availability Zone.
  count = var.availability_zone_count

  # Select the public subnet in the same list position.
  subnet_id = aws_subnet.public[count.index].id

  # Attach that subnet to the public route table.
  route_table_id = aws_route_table.public.id
}

# Create one private route table per Availability Zone.
resource "aws_route_table" "private" {
  # Create one private route table for each private subnet.
  count = var.availability_zone_count

  # Attach this route table to the Atlas VPC.
  vpc_id = aws_vpc.main.id

  # Do not add an internet route here yet.
  # nat.tf will add it only when NAT Gateway is enabled.

  tags = merge(
    local.common_tags,
    {
      # Example: atlas-commerce-dev-private-1-rt.
      Name = "${local.name_prefix}-private-${count.index + 1}-rt"

      # Identify this route table as private.
      Tier = "private"
    }
  )
}

# Associate every private subnet with its matching private route table.
resource "aws_route_table_association" "private" {
  # Create one association for each selected Availability Zone.
  count = var.availability_zone_count

  # Select the private subnet in the same list position.
  subnet_id = aws_subnet.private[count.index].id

  # Attach that subnet to the matching private route table.
  route_table_id = aws_route_table.private[count.index].id
}