# Create one public subnet in each selected Availability Zone.
resource "aws_subnet" "public" {
  # Create as many public subnets as selected AZs.
  count = var.availability_zone_count

  # Place this subnet inside the VPC created in vpc.tf.
  vpc_id = aws_vpc.main.id

  # Use the CIDR at the same position in the public CIDR list.
  cidr_block = var.public_subnet_cidrs[count.index]

  # Place this subnet in the matching Availability Zone.
  availability_zone = local.availability_zones[count.index]

  # Give public IPs to resources launched here when applicable.
  map_public_ip_on_launch = true

  tags = merge(
    local.common_tags,
    {
      # Example: atlas-commerce-dev-public-1.
      Name = "${local.name_prefix}-public-${count.index + 1}"

      # Identify this subnet's intended network tier.
      Tier = "public"

      "kubernetes.io/role/elb" = "1"
    }
  )
}

# Create one private subnet in each selected Availability Zone.
resource "aws_subnet" "private" {
  # Create as many private subnets as selected AZs.
  count = var.availability_zone_count

  # Place this subnet inside the VPC created in vpc.tf.
  vpc_id = aws_vpc.main.id

  # Use the CIDR at the same position in the private CIDR list.
  cidr_block = var.private_subnet_cidrs[count.index]

  # Place this subnet in the matching Availability Zone.
  availability_zone = local.availability_zones[count.index]

  # Do not automatically assign public IPs to resources in this subnet.
  map_public_ip_on_launch = false

  tags = merge(
    local.common_tags,
    {
      # Example: atlas-commerce-dev-private-1.
      Name = "${local.name_prefix}-private-${count.index + 1}"

      # Identify this subnet's intended network tier.
      Tier = "private"

      "kubernetes.io/role/elb" = "1"
    }
  )
}