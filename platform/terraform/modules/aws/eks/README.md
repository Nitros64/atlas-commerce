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

## Optional workload IAM

- `enable_irsa` enables the OIDC provider and the External Secrets role used by
  the DEV bootstrap.
- `enable_aws_load_balancer_controller_irsa` creates the controller role only
  when the controller is intentionally enabled. It defaults to `false`.
- `enable_velero_irsa` creates the Velero role only when Velero is intentionally
  enabled. It defaults to `false`.

The module creates IAM integrations, not the Kubernetes controllers themselves.
Callers must keep these flags aligned with the add-ons they actually install.

## Network Design

```text
Public subnets
  └── Future ALB / NAT Gateway

Private subnets
  └── EKS control plane ENIs
  └── EKS worker nodes
  └── Kubernetes workloads
```
