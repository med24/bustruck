. "$PSScriptRoot\common.ps1"
Write-Section '06 - Resilience (this script stops and restarts containers)'

function Get-BreakerState($name) {
    $r = Invoke-Http 'GET' "$TICKET/actuator/circuitbreakers"
    if ($r.Json -and $r.Json.circuitBreakers.$name) { return $r.Json.circuitBreakers.$name.state }
    return 'UNKNOWN'
}

try {
    # ---- 1. payment-service down ----
    Write-Host '  -> stopping payment-service' -ForegroundColor DarkGray
    Compose stop payment-service | Out-Null

    $sw = [Diagnostics.Stopwatch]::StartNew()
    $r = Buy 1 '4111111111111111'
    $sw.Stop()
    Check '503 PAYMENT_UNAVAILABLE when payment is down' ($r.Status -eq 503 -and $r.Json.code -eq 'PAYMENT_UNAVAILABLE') "HTTP $($r.Status) $($r.Body)"
    Check 'response carries the PENDING ticket id' ($r.Json.ticketId -and $r.Json.ticketStatus -eq 'PENDING') $r.Body
    Write-Host ("     (took {0:N1}s thanks to 3 retries with backoff)" -f $sw.Elapsed.TotalSeconds) -ForegroundColor DarkGray

    $pendingId = $r.Json.ticketId
    $t = Api-Get "/tickets/$pendingId"
    Check 'ticket was kept PENDING in the database' ($t.Status -eq 200 -and $t.Json.status -eq 'PENDING') $t.Body

    # open the breaker (needs >= 5 failed calls; each purchase makes 3 attempts)
    1..3 | ForEach-Object { Buy 1 '4111111111111111' | Out-Null }
    Check 'circuit breaker is OPEN' ((Get-BreakerState 'payment') -eq 'OPEN') "state = $(Get-BreakerState 'payment')"

    $sw = [Diagnostics.Stopwatch]::StartNew()
    $fast = Buy 1 '4111111111111111'
    $sw.Stop()
    Check 'open breaker fails fast (< 1.5 s) with 503' ($fast.Status -eq 503 -and $sw.Elapsed.TotalSeconds -lt 1.5) ("{0:N1}s" -f $sw.Elapsed.TotalSeconds)

    # ---- 2. recovery ----
    Write-Host '  -> restarting payment-service (waiting for it and for the breaker, ~30-60 s)' -ForegroundColor DarkGray
    Compose start payment-service | Out-Null
    Check 'payment-service is healthy again' (Wait-Healthy "$PAYMENT/actuator/health" 90)
    Start-Sleep -Seconds 21      # wait-duration-in-open-state = 20 s

    $a = Buy 1 '4111111111111111'
    $b = Buy 1 '4111111111111111'
    Check 'purchases succeed again after recovery' ($a.Status -eq 201 -and $b.Status -eq 201) "HTTP $($a.Status) / $($b.Status)"
    Check 'circuit breaker is CLOSED again' ((Get-BreakerState 'payment') -eq 'CLOSED') "state = $(Get-BreakerState 'payment')"

    # ---- 3. tracking-service down ----
    Write-Host '  -> stopping tracking-service' -ForegroundColor DarkGray
    Compose stop tracking-service | Out-Null

    $nb = Api-Get '/lines/1/next-bus'
    Check 'next-bus degrades gracefully (200, realTime=false)' ($nb.Status -eq 200 -and $nb.Json.realTime -eq $false -and $nb.Json.message -match 'unavailable') "HTTP $($nb.Status) $($nb.Body)"

    $buy = Buy 1 '4111111111111111'
    Check 'ticket purchase keeps working while tracking is down' ($buy.Status -eq 201 -and $buy.Json.status -eq 'PAID') "HTTP $($buy.Status) $($buy.Body)"

    Write-Host '  -> restarting tracking-service' -ForegroundColor DarkGray
    Compose start tracking-service | Out-Null
    $back = Wait-Until { $r = Api-Get '/lines/1/next-bus'; $r.Status -eq 200 -and $r.Json.realTime -eq $true } 120 4
    Check 'next-bus is real-time again after tracking restarts' $back

    # ---- 4. ticket-service down: gateway behaviour ----
    Write-Host '  -> stopping ticket-service' -ForegroundColor DarkGray
    Compose stop ticket-service | Out-Null
    $gw = Api-Get '/lines'
    Check 'gateway returns a 5xx when ticket-service is down' ($gw.Status -ge 500 -and $gw.Status -lt 600) "HTTP $($gw.Status)"
    Compose start ticket-service | Out-Null
    Check 'ticket-service is back' (Wait-Healthy "$TICKET/actuator/health" 90)
}
finally {
    Write-Host '  -> making sure every service is running again' -ForegroundColor DarkGray
    Compose start payment-service tracking-service ticket-service | Out-Null
}

Show-Summary
