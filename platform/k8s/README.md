# LEGACY Kubernetes manifests

This directory is retained only as historical reference.

It is not part of the current deployment flow, must not be used as a manifest
source, and must not be synchronized by CI or GitOps tooling.

The current source of truth for Kubernetes application resources is:

    platform/helm/atlas-commerce

New Kubernetes changes belong in the Helm chart. Do not copy or reuse manifests
from this directory when implementing Helm, Argo CD or CI/CD work.
