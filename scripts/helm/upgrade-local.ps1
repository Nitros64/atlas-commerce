$ErrorActionPreference = "Stop"

$ReleaseName = "atlas"
$Namespace = "atlas-helm"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Resolve-Path "$ScriptDir/../.."
$ChartDir = "$ProjectRoot/platform/helm/atlas-commerce"

$ValuesFiles = @(
    "$ChartDir/values/auth.yaml",
    "$ChartDir/values/catalog.yaml",
    "$ChartDir/values/cart.yaml",
    "$ChartDir/values/gateway.yaml",
    "$ChartDir/values/kafka.yaml",
    "$ChartDir/values/postgres.yaml",
    "$ChartDir/values/redis.yaml",
    "$ChartDir/values/secrets-local.yaml"
)

Write-Host "Upgrading Helm release..."
Write-Host "Release:   $ReleaseName"
Write-Host "Namespace: $Namespace"
Write-Host "Chart:     $ChartDir"
Write-Host ""

$HelmArgs = @()

foreach ($file in $ValuesFiles) {
    if (-not (Test-Path $file)) {
        Write-Error "ERROR: values file not found: $file"
        exit 1
    }

    $HelmArgs += @("-f", $file)
}

helm upgrade --install $ReleaseName $ChartDir `
    -n $Namespace `
    --create-namespace `
    @HelmArgs

Write-Host ""
Write-Host "Helm upgrade completed."
Write-Host ""

kubectl get pods -n $Namespace