Write-Host "Stopping Atlas infrastructure..." -ForegroundColor Cyan

docker compose `
  -f .\platform\docker\docker-compose.dev.yml `
  down

Write-Host ""
Write-Host "Atlas infrastructure stopped." -ForegroundColor Green