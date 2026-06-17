#!/usr/bin/env bash
set -euo pipefail

RELEASE_NAME="atlas"
NAMESPACE="atlas-helm"

echo "Helm release status..."
echo "Release:   $RELEASE_NAME"
echo "Namespace: $NAMESPACE"
echo

helm status "$RELEASE_NAME" -n "$NAMESPACE"

echo
echo "Pods:"
kubectl get pods -n "$NAMESPACE"

echo
echo "Services:"
kubectl get svc -n "$NAMESPACE"

echo
echo "PVCs:"
kubectl get pvc -n "$NAMESPACE"

echo
echo "Helm history:"
helm history "$RELEASE_NAME" -n "$NAMESPACE"