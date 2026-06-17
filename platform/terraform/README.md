# Atlas Commerce Terraform Platform

Terraform infrastructure for Atlas Commerce.

## Structure

- `bootstrap/`: one-time infrastructure for Terraform remote state.
- `live/`: environment-specific compositions.
- `modules/`: reusable cloud infrastructure modules.
- `modules-kubernetes/`: reusable Kubernetes add-on modules.
- `policies/`: IAM and policy-as-code documents.
- `scripts/`: local helper scripts.
- `docs/`: operational documentation.

## Principles

- Terraform provisions cloud infrastructure.
- Helm deploys Atlas Commerce applications.
- Environments use isolated state files.
- No Terraform workspaces for dev/staging/prod isolation.
- Secrets and tfvars are not committed.
