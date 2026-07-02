# AWS EKS Module

Reusable Terraform module that defines the Atlas Commerce EKS platform.

## Resources

- EKS control plane
- Managed node group in private subnets
- IAM roles for cluster and worker nodes
- OIDC provider for IRSA
- Dedicated IAM role for the Amazon VPC CNI
- Managed add-ons:
  - vpc-cni
  - coredns
  - kube-proxy

## Network Design

```text
Public subnets
  └── Future ALB / NAT Gateway

Private subnets
  └── EKS control plane ENIs
  └── EKS worker nodes
  └── Kubernetes workloads