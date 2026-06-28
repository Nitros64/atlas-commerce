# Define the VPC where all security groups will be created.
variable "vpc_id" {
  description = "ID of the VPC where Atlas security groups will be created."
  type        = string
}

# Define the project name used for resource names and tags.
variable "project" {
  description = "Project name used for resource names and tags."
  type        = string
}

# Define the environment name used for resource names and tags.
variable "environment" {
  description = "Environment name, for example dev, staging or prod."
  type        = string
}

# Define which IPv4 ranges may reach the public Application Load Balancer.
variable "alb_ingress_cidrs" {
  description = "IPv4 CIDR blocks allowed to reach the public ALB on ports 80 and 443."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

# Allow callers to add extra tags without changing this module.
variable "additional_tags" {
  description = "Additional tags applied to Atlas security groups."
  type        = map(string)
  default     = {}
}