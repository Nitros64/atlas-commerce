# Define the project name used for resource names and tags.
variable "project" {
  description = "Project name used for resource names and tags."
  type        = string
}

# Define the environment name, for example dev, staging, or prod.
variable "environment" {
  description = "Environment name used for resource names and tags."
  type        = string
}

# Define the EKS cluster name.
variable "cluster_name" {
  description = "Name assigned to the EKS cluster."
  type        = string
}

# Define the Kubernetes version explicitly to avoid unexpected upgrades.
variable "kubernetes_version" {
  description = "Kubernetes version used by the EKS control plane."
  type        = string
}

# Define the VPC where the EKS cluster will run.
variable "vpc_id" {
  description = "ID of the VPC where the EKS cluster will be created."
  type        = string
}

# Define the private subnets used by the EKS control plane and worker nodes.
variable "private_subnet_ids" {
  description = "Private subnet IDs used by the EKS cluster and managed node group."
  type        = list(string)
}

# Allow optional extra security groups for EKS control-plane ENIs.
variable "cluster_security_group_ids" {
  description = "Additional security group IDs attached to EKS control-plane network interfaces."
  type        = list(string)
  default     = []
}

# Control whether the Kubernetes API is reachable from inside the VPC.
variable "cluster_endpoint_private_access" {
  description = "Whether the Kubernetes API endpoint is reachable privately inside the VPC."
  type        = bool
}

# Control whether the Kubernetes API is reachable from the public internet.
variable "cluster_endpoint_public_access" {
  description = "Whether the Kubernetes API endpoint is reachable publicly."
  type        = bool
}

# Limit which public IPv4 ranges can reach the Kubernetes API endpoint.
variable "cluster_endpoint_public_access_cidrs" {
  description = "IPv4 CIDR blocks allowed to reach the public Kubernetes API endpoint."
  type        = list(string)
  default     = []
}

# Define optional EKS control-plane log types sent to CloudWatch.
variable "enabled_cluster_log_types" {
  description = "EKS control-plane log types enabled for this cluster."
  type        = list(string)
  default     = []
}

# Enable the OIDC provider required later for IRSA.
variable "enable_irsa" {
  description = "Whether to create the OIDC provider used by IAM Roles for Service Accounts."
  type        = bool
  default     = true
}

# Define the managed node group name.
variable "node_group_name" {
  description = "Name assigned to the default EKS managed node group."
  type        = string
  default     = "default"
}

# Define the EC2 instance types allowed for worker nodes.
variable "node_instance_types" {
  description = "EC2 instance types used by the managed node group."
  type        = list(string)
}

# Define whether nodes use On-Demand or Spot capacity.
variable "node_capacity_type" {
  description = "Capacity type for worker nodes: ON_DEMAND or SPOT."
  type        = string
  default     = "ON_DEMAND"

  validation {
    condition     = contains(["ON_DEMAND", "SPOT"], var.node_capacity_type)
    error_message = "node_capacity_type must be ON_DEMAND or SPOT."
  }
}

# Define the minimum number of worker nodes.
variable "node_min_size" {
  description = "Minimum number of worker nodes."
  type        = number
}

# Define the desired number of worker nodes at creation time.
variable "node_desired_size" {
  description = "Desired number of worker nodes."
  type        = number
}

# Define the maximum number of worker nodes.
variable "node_max_size" {
  description = "Maximum number of worker nodes."
  type        = number
}

# Define the root EBS volume size for each worker node.
variable "node_disk_size_gib" {
  description = "Root EBS volume size in GiB for each worker node."
  type        = number
  default     = 20
}

# Define the EKS-optimized AMI family used by worker nodes.
variable "node_ami_type" {
  description = "AMI type used by the managed node group."
  type        = string
  default     = "AL2023_x86_64_STANDARD"
}

# Allow environment-specific tags without changing module code.
variable "additional_tags" {
  description = "Additional tags applied to EKS resources."
  type        = map(string)
  default     = {}
}

variable "enable_velero_irsa" {
  description = "Whether to create the IAM role for Velero using IRSA."
  type        = bool
  default     = false
}

variable "velero_namespace" {
  description = "Kubernetes namespace where Velero will run."
  type        = string
  default     = "velero"
}

variable "velero_service_account_name" {
  description = "Kubernetes ServiceAccount name used by Velero."
  type        = string
  default     = "velero"
}

variable "velero_backup_bucket_arn" {
  description = "S3 bucket ARN used by Velero to store backups."
  type        = string
  default     = null
}