Write-Host "Starting Atlas microservices locally..." -ForegroundColor Cyan

$root = Split-Path -Parent $PSScriptRoot

function Start-Service {
    param (
        [string]$Name,
        [string]$Path,
        [hashtable]$EnvVars = @{}
    )

    $servicePath = Join-Path $root $Path

    $envCommands = " `$env:SPRING_PROFILES_ACTIVE='local'; "

    foreach ($key in $EnvVars.Keys) {
        $envCommands += " `$env:$key='$($EnvVars[$key])'; "
    }

    $command = "cd '$servicePath'; $envCommands mvn spring-boot:run"

    Write-Host "Starting $Name..." -ForegroundColor Green

    Start-Process powershell -ArgumentList "-NoExit", "-Command", $command
}

Start-Service "auth-service" "services/auth-service"
Start-Sleep -Seconds 3

Start-Service "catalog-service" "services/catalog-service"
Start-Sleep -Seconds 3

Start-Service "cart-service" "services/cart-service"
Start-Sleep -Seconds 3

Start-Service "coupon-service" "services/coupon-service"
Start-Sleep -Seconds 3

Start-Service "pricing-service" "services/pricing-service"
Start-Sleep -Seconds 3

Start-Service "inventory-service" "services/inventory-service"
Start-Sleep -Seconds 3

Start-Service "order-service" "services/order-service"
Start-Sleep -Seconds 3

Start-Service "payment-service" "services/payment-service"
Start-Sleep -Seconds 3

Start-Service "shipping-service" "services/shipping-service"
Start-Sleep -Seconds 3

Start-Service "notification-service" "services/notification-service" @{
    SERVER_PORT = "8091"
}
Start-Sleep -Seconds 3

Start-Service "audit-service" "services/audit-service"
Start-Sleep -Seconds 5

Start-Service "gateway-service" "services/gateway-service"

Write-Host ""
Write-Host "All Atlas services are starting." -ForegroundColor Cyan