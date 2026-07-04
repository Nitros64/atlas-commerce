# AWS ECR Module

Reusable Terraform module that creates private Amazon ECR repositories for Atlas Commerce services.

## Features

- One repository per service
- Immutable image tags
- Scan on push
- AES256 encryption at rest
- Lifecycle policy for untagged images
- Lifecycle policy that keeps only a limited number of `sha-` tagged images

## Expected CI Image Tags

GitHub Actions should publish immutable tags such as:

```text
sha-a1b2c3d4