variable "aws_region" {
  type    = string
  default = "eu-central-1"
}

variable "project" {
  type    = string
  default = "atlas-commerce"
}

variable "environment" {
  type    = string
  default = "shared"
}

variable "repository_prefix" {
  description = "Namespace placed before each ECR repository name."
  type        = string
  default     = "atlas-commerce"
}

variable "repository_names" {
  description = "Names of the ECR repositories to create."
  type        = set(string)
}

variable "scan_on_push" {
  type    = bool
  default = true
}

variable "image_tag_mutability" {
  type    = string
  default = "IMMUTABLE"

  validation {
    condition     = contains(["IMMUTABLE", "MUTABLE"], var.image_tag_mutability)
    error_message = "image_tag_mutability must be IMMUTABLE or MUTABLE."
  }
}

variable "max_tagged_images" {
  type    = number
  default = 20
}

variable "untagged_image_expiration_days" {
  type    = number
  default = 7
}

variable "additional_tags" {
  type    = map(string)
  default = {}
}