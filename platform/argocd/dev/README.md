# Atlas DEV with Argo CD

This directory owns the single Argo CD `Application` and restricted
`AppProject` for Atlas DEV. The application renders the existing Helm chart;
Helm remains the source of truth for Kubernetes resources.

## Prerequisites

- A kubeconfig for the DEV cluster available only to the operator.
- External Secrets Operator already installed with its CRDs and service account.
- The non-secret Terraform outputs for RDS, Redis and the RDS master secret ARN.

Before applying the Application, replace every `REPLACE_WITH_...` value in
`application.yaml` in Git and merge it to `master`. Do not put secret values in
this file. The ECR registry is the only account-specific image setting.

## Install or update Argo CD

Run the pinned, idempotent bootstrap script from an operator workstation:

```bash
bash scripts/argocd/install-argocd-dev.sh
```

The script installs only Argo CD. It does not install Atlas with Helm.

## Validate without a cluster

The validation uses local `kubectl kustomize` and `helm template`; it does not
read kubeconfig, contact AWS or call a Kubernetes API:

```bash
bash scripts/argocd/validate-argocd-dev.sh
```

## Register and synchronize Atlas DEV

Apply the project before the application, then synchronize manually:

```bash
kubectl apply -f platform/argocd/dev/app-project.yaml
kubectl apply -f platform/argocd/dev/application.yaml
argocd app sync atlas-dev
argocd app wait atlas-dev --health --sync
```

There is intentionally no `automated` sync policy. Auto-sync may be introduced
later, initially without prune. CI must never receive kubeconfig or invoke the
Argo CD API; CI only performs offline validation.

## Promote one DEV image

Run the **Promote DEV image** workflow manually from GitHub Actions. Select one
of the twelve active services and enter the immutable ECR tag emitted by its
service CI, for example `sha-<40-character-commit>-<run-id>-<attempt>`.

The workflow is protected by the GitHub `dev` Environment. It validates the
service and tag, changes exactly one `tag` in
`values/images.dev.yaml`, and opens a pull request against `master`. It uses
only `contents: write` and `pull-requests: write`; it has no AWS, OIDC or cluster
permissions. The repository setting **Allow GitHub Actions to create and
approve pull requests** must be enabled. Pull-request checks created with the
repository token may require approval from a user with write access.

Merging the pull request changes desired state in Git. Argo CD notices the new
commit and performs the deployment; the promotion workflow never performs a
deployment itself. Automated rollback is intentionally deferred. Until then,
revert a promotion with a normal `git revert` and let Argo CD reconcile Git.

## Ownership and ordering

The chart temporarily owns `ClusterSecretStore/aws-secrets-manager` because
moving it to a separate platform application would expand this delivery. Its
wave is `-30`, before the namespaced `ExternalSecret` resources at `-20`.
The PostgreSQL bootstrap waits explicitly for generated Secrets and runs at
`-10`. Kafka runs at `10`, backend deployments at `20`, and the gateway and
gateway at `30`. Ingress is disabled for DEV because no ingress controller is
part of the supported platform; use the runbook's gateway port-forward.
