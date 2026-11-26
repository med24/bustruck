. "$PSScriptRoot\common.ps1"
Write-Section '01 - Health of all services'

$targets = @{
    'api-gateway'      = "$GW/actuator/health"
    'ticket-service'   = "$TICKET/actuator/health"
    'tracking-service' = "$TRACK/actuator/health"
    'payment-service'  = "$PAYMENT/actuator/health"
}
foreach ($name in $targets.Keys) {
    $r = Invoke-Http 'GET' $targets[$name]
    $status = ''; if ($r.Json) { $status = $r.Json.status }
    Check "$name is UP" ($r.Status -eq 200 -and $status -eq 'UP') "HTTP $($r.Status) $($r.Body)"
}

$wsdl = Invoke-Http 'GET' "$PAYMENT/ws/payment.wsdl"
Check 'payment WSDL is published' ($wsdl.Status -eq 200 -and $wsdl.Body -match 'AuthorizePayment') "HTTP $($wsdl.Status)"

$docs = Invoke-Http 'GET' "$GW/v3/api-docs"
Check 'OpenAPI docs reachable through the gateway' ($docs.Status -eq 200 -and $docs.Body -match '/api/v1/tickets') "HTTP $($docs.Status)"

Show-Summary
