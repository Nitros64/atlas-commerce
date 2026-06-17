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
    "$ChartDir/values/pricing.yaml",
    "$ChartDir/values/coupon.yaml",
    "$ChartDir/values/gateway.yaml",
    "$ChartDir/values/kafka.yaml",
    "$ChartDir/values/postgres.yaml",
    "$ChartDir/values/redis.yaml",
    "$ChartDir/values/secrets-local.yaml"
)

Write-Host "Rendering Helm chart..."
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

helm template $ReleaseName $ChartDir `
    -n $Namespace `
    @HelmArgs