. "$PSScriptRoot\common.ps1"
Write-Section '03 - gRPC: tracking-service (called directly, grpcurl via Docker)'

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host '  docker not found - skipping' -ForegroundColor Yellow; Show-Summary; return
}

# Wait for the simulator to have produced positions
$ready = Wait-Until { (Invoke-Grpc 'GetLastPosition' '{"bus_id":"BUS-101"}' 5) -match 'BUS-101' } 40 3
Check 'simulator buses are present' $ready

# Unary
$r = Invoke-Grpc 'GetLastPosition' '{"bus_id":"BUS-101"}'
Check 'GetLastPosition returns the bus' ($r -match 'BUS-101' -and $r -match 'latitude') $r

$r = Invoke-Grpc 'GetLineLastPosition' '{"line_code":"L10"}'
Check 'GetLineLastPosition returns a bus of L10' ($r -match '"lineCode": "L10"') $r

$r = Invoke-Grpc 'GetLastPosition' '{"bus_id":"NOPE"}'
Check 'unknown bus returns NotFound' ($r -match 'NotFound') $r

# Client streaming: report 2 positions
$msgs = @(
  '{"bus_id":"BUS-TEST","line_code":"LT","latitude":36.80,"longitude":10.18,"speed_kmh":30}',
  '{"bus_id":"BUS-TEST","line_code":"LT","latitude":36.81,"longitude":10.19,"speed_kmh":35}'
)
$r = Invoke-Grpc 'ReportPositions' $msgs
Check 'ReportPositions (client streaming) acks 2 positions' ($r -match '"receivedCount": "2"') $r

$r = Invoke-Grpc 'GetLastPosition' '{"bus_id":"BUS-TEST"}'
Check 'reported position is stored (last = 36.81)' ($r -match '36\.81') $r

# Server streaming: ~7 s of stream on L10 (2 buses, push every 2 s) => several messages
$r = Invoke-Grpc 'StreamLinePositions' '{"line_code":"L10"}' 7
$count = ([regex]::Matches($r, '"busId"')).Count
Check "StreamLinePositions (server streaming) pushed multiple updates ($count messages)" ($count -ge 4) $r

Show-Summary
