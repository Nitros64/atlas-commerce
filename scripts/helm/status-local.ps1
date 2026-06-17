$ErrorActionPreference = "Stop"

$ReleaseName = "atlas"
$Namespace = "atlas-helm"

Write-Host "Helm release status..."
Write-Host "Release:   $ReleaseName"
Write-Host "Namespace: $Namespace"
Write-Host ""

helm status $ReleaseName -n $Namespace

Write-Host ""
Write-Host "Pods:"
kubectl get pods -n $Namespace

Write-Host ""
Write-Host "Services:"
kubectl get svc -n $Namespace

Write-Host ""
Write-Host "PVCs:"
kubectl get pvc -n $Namespace

Write-Host ""
Write-Host "Helm history:"
helm history $ReleaseName -n $Namespace