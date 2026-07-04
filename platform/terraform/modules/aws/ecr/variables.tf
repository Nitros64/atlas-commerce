variable "project" {
  type = string
}

variable "environment" {
  type = string
}

variable "repository_prefix" {
  type = string
}

variable "repository_names" {
  type = set(string)
}

variable "scan_on_push" {
  type    = bool
  default = true
}

variable "image_tag_mutability" {
  type    = string
  default = "IMMUTABLE"
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