# Allow HTTP traffic from approved IPv4 CIDR ranges to the public ALB.
resource "aws_vpc_security_group_ingress_rule" "alb_http" {
  # Create one HTTP rule for every allowed IPv4 CIDR range.
  for_each = toset(var.alb_ingress_cidrs)

  # Add this inbound rule to the ALB security group.
  security_group_id = aws_security_group.alb.id

  # Allow traffic from the current allowed IPv4 CIDR range.
  cidr_ipv4 = each.value

  # Allow standard HTTP traffic.
  from_port = 80
  to_port   = 80

  # Restrict the rule to TCP.
  ip_protocol = "tcp"

  # Describe the purpose of the rule in AWS.
  description = "Allow HTTP traffic to the Atlas public ALB."
}

# Allow HTTPS traffic from approved IPv4 CIDR ranges to the public ALB.
resource "aws_vpc_security_group_ingress_rule" "alb_https" {
  # Create one HTTPS rule for every allowed IPv4 CIDR range.
  for_each = toset(var.alb_ingress_cidrs)

  # Add this inbound rule to the ALB security group.
  security_group_id = aws_security_group.alb.id

  # Allow traffic from the current allowed IPv4 CIDR range.
  cidr_ipv4 = each.value

  # Allow standard HTTPS traffic.
  from_port = 443
  to_port   = 443

  # Restrict the rule to TCP.
  ip_protocol = "tcp"

  # Describe the purpose of the rule in AWS.
  description = "Allow HTTPS traffic to the Atlas public ALB."
}