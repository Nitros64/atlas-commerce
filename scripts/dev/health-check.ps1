param(
[switch]$Detailed
)

$services = @(
@{ Name = "gateway";      Port = 8080 },
@{ Name = "auth";         Port = 8081 },
@{ Name = "catalog";      Port = 8082 },
@{ Name = "order";        Port = 8083 },
@{ Name = "inventory";    Port = 8084 },
@{ Name = "cart";         Port = 8085 },
@{ Name = "pricing";      Port = 8086 },
@{ Name = "coupon";       Port = 8087 },
@{ Name = "payment";      Port = 8088 },
@{ Name = "shipping";     Port = 8089 },
@{ Name = "notification"; Port = 8091 },
@{ Name = "audit";        Port = 8093 }
)

Write-Host ""
Write-Host "Atlas Health Check" -ForegroundColor Cyan
Write-Host "==================" -ForegroundColor Cyan
Write-Host ""

$success = 0
$failed = 0

foreach ($service in $services) {


$url = "http://localhost:$($service.Port)/actuator/health"

try {

    $response = Invoke-RestMethod `
        -Uri $url `
        -Method Get `
        -TimeoutSec 5

    if ($response.status -eq "UP") {

        Write-Host "[UP]   $($service.Name) ($($service.Port))" -ForegroundColor Green
        $success++

        if ($Detailed) {
            $response | ConvertTo-Json -Depth 10
            Write-Host ""
        }

    }
    else {

        Write-Host "[DOWN] $($service.Name) ($($service.Port))" -ForegroundColor Yellow
        $failed++
    }

}
catch {

    Write-Host "[FAIL] $($service.Name) ($($service.Port))" -ForegroundColor Red
    $failed++
}


}

Write-Host ""
Write-Host "==================" -ForegroundColor Cyan
Write-Host "Healthy services : $success" -ForegroundColor Green
Write-Host "Failed services  : $failed" -ForegroundColor Red
Write-Host "==================" -ForegroundColor Cyan

if ($failed -eq 0) {
Write-Host ""
Write-Host "Atlas is fully operational." -ForegroundColor Green
}
