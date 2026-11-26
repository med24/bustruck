<#
 Runs every BusTrack test script in order and exits with code 1 if anything failed.
 Usage (from the bustrack/ folder):
   powershell -ExecutionPolicy Bypass -File .\tests\run-all.ps1
   powershell -ExecutionPolicy Bypass -File .\tests\run-all.ps1 -SkipResilience    # does not stop containers
   powershell -ExecutionPolicy Bypass -File .\tests\run-all.ps1 -SkipGrpc          # no docker/grpcurl needed
#>
param([switch]$SkipResilience, [switch]$SkipGrpc)

. "$PSScriptRoot\common.ps1"
$global:TestPass = 0; $global:TestFail = 0

Write-Host 'Waiting for the gateway to be ready...' -ForegroundColor DarkGray
if (-not (Wait-Healthy "$GW/actuator/health" 120)) {
    Write-Host 'Gateway not reachable on :8080 - run "docker compose up --build -d" first.' -ForegroundColor Red
    exit 1
}

& "$PSScriptRoot\01-health.ps1"
& "$PSScriptRoot\02-soap.ps1"
if (-not $SkipGrpc) { & "$PSScriptRoot\03-grpc.ps1" }
& "$PSScriptRoot\04-rest.ps1"
& "$PSScriptRoot\05-purchase.ps1"
if (-not $SkipResilience) { & "$PSScriptRoot\06-resilience.ps1" }

Write-Host "`n==================== TOTAL ====================" -ForegroundColor Cyan
Show-Summary
if ($global:TestFail -gt 0) { exit 1 } else { exit 0 }
