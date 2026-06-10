Write-Host "Resetting Atlas..." -ForegroundColor Red

Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force

docker compose `
  -f .\platform\docker\docker-compose.dev.yml `
  down -v

Write-Host ""
Write-Host "Atlas reset complete." -ForegroundColor Green