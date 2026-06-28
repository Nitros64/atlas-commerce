variable "project" {
  description = "Project name used for resource names and tags."
  type        = string
}

variable "environment" {
  description = "Environment name, for example dev, staging or prod."
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block assigned to the VPC."
  type        = string
}

variable "availability_zone_count" {
  description = "Number of Availability Zones to use."
  type        = number
  default     = 2

  validation {
    condition     = var.availability_zone_count >= 2 && var.availability_zone_count <= 3
    error_message = "availability_zone_count must be between 2 and 3."
  }
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets. One per Availability Zone."
  type        = list(string)
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets. One per Availability Zone."
  type        = list(string)
}

variable "enable_nat_gateway" {
  description = "Whether to create a NAT Gateway for private subnet outbound internet access."
  type        = bool
  default     = false
}

variable "additional_tags" {
  description = "Additional tags applied to all network resources."
  type        = map(string)
  default     = {}
}