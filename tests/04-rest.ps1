. "$PSScriptRoot\common.ps1"
Write-Section '04 - REST through the gateway: lines and next-bus'

$lines = Api-Get '/lines'
Check 'GET /lines returns 200 and at least 2 lines' ($lines.Status -eq 200 -and @($lines.Json).Count -ge 2) "HTTP $($lines.Status) $($lines.Body)"
Check 'gateway adds X-Correlation-Id' ($lines.Headers.ContainsKey('X-Correlation-Id'))

$lineId = @($lines.Json)[0].id
$one = Api-Get "/lines/$lineId"
Check 'GET /lines/{id} returns the line' ($one.Status -eq 200 -and $one.Json.id -eq $lineId)

$nf = Api-Get '/lines/999999'
Check 'unknown line returns 404' ($nf.Status -eq 404 -and $nf.Json.code -eq 'NOT_FOUND') "HTTP $($nf.Status)"

# Create a line
$code = 'T' + (Get-Random -Maximum 99999)
$new = Api-Post '/lines' @{ code = $code; name = 'Test line'; price = 3.0; stopLatitude = 36.8; stopLongitude = 10.1 }
Check 'POST /lines returns 201 with Location' ($new.Status -eq 201 -and $new.Location -match '/api/v1/lines/\d+') "HTTP $($new.Status) $($new.Body)"

$bad = Api-Post '/lines' @{ code = ''; name = '' }
Check 'invalid line returns 400 VALIDATION_ERROR' ($bad.Status -eq 400 -and $bad.Json.code -eq 'VALIDATION_ERROR') "HTTP $($bad.Status) $($bad.Body)"

# next-bus (REST -> gRPC); simulator needs a few seconds after startup
$ok = Wait-Until { $r = Api-Get '/lines/1/next-bus'; $r.Status -eq 200 -and $r.Json.realTime -eq $true } 40 3
$nb = Api-Get '/lines/1/next-bus'
Check 'next-bus is real-time with bus, position and ETA' ($ok -and $nb.Json.busId -and $nb.Json.etaSeconds -ge 0) $nb.Body

$none = Api-Get "/lines/$($new.Json.id)/next-bus"
Check 'next-bus on a line without buses returns 404' ($none.Status -eq 404) "HTTP $($none.Status) $($none.Body)"

Show-Summary
