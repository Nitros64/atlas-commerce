Write-Host "Stopping Atlas services..." -ForegroundColor Cyan

Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force

Write-Host ""
Write-Host "Atlas services stopped." -ForegroundColor Green