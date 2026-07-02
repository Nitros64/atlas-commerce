variable "eks_cluster_name" {
  type    = string
  default = "atlas-commerce-dev"
}

variable "eks_kubernetes_version" {
  type = string
}

variable "eks_cluster_endpoint_public_access_cidrs" {
  type = list(string)
}

variable "eks_node_instance_types" {
  type = list(string)
}

variable "eks_node_capacity_type" {
  type    = string
  default = "ON_DEMAND"
}

variable "eks_node_min_size" {
  type    = number
  default = 2
}

variable "eks_node_desired_size" {
  type    = number
  default = 2
}

variable "eks_node_max_size" {
  type    = number
  default = 3
}

variable "eks_node_disk_size_gib" {
  type    = number
  default = 20
}