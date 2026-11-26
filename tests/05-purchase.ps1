. "$PSScriptRoot\common.ps1"
Write-Section '05 - Ticket purchase (REST -> SOAP) and ticket lifecycle'

# Successful purchase
$p = Buy 1 '4111111111111111'
Check 'purchase returns 201 PAID with paymentId' ($p.Status -eq 201 -and $p.Json.status -eq 'PAID' -and $p.Json.paymentId) "HTTP $($p.Status) $($p.Body)"
Check 'Location header points to the ticket' ($p.Location -match '/api/v1/tickets/\d+') $p.Location
$id = $p.Json.id

$g = Api-Get "/tickets/$id"
Check 'GET /tickets/{id} returns PAID' ($g.Status -eq 200 -and $g.Json.status -eq 'PAID')

# Validate
$v = Api-Post "/tickets/$id/validate"
Check 'validate PAID ticket -> USED' ($v.Status -eq 200 -and $v.Json.status -eq 'USED' -and $v.Json.validatedAt) $v.Body
$v2 = Api-Post "/tickets/$id/validate"
Check 'validating twice returns 409' ($v2.Status -eq 409 -and $v2.Json.code -eq 'INVALID_TICKET_STATE') "HTTP $($v2.Status)"

# SOAP Fault -> 422
$d = Buy 1 '4000000000000002'
Check 'declined card returns 422 CARD_DECLINED' ($d.Status -eq 422 -and $d.Json.code -eq 'CARD_DECLINED') "HTTP $($d.Status) $($d.Body)"

$i = Buy 1 '12345'
Check 'invalid card returns 422 INVALID_CARD' ($i.Status -eq 422 -and $i.Json.code -eq 'INVALID_CARD') "HTTP $($i.Status) $($i.Body)"

# Input errors
$nl = Buy 999999 '4111111111111111'
Check 'unknown line returns 404' ($nl.Status -eq 404) "HTTP $($nl.Status)"
$bl = Api-Post '/tickets' @{ lineId = 1; passengerName = ''; cardNumber = '4111111111111111' }
Check 'blank passenger returns 400' ($bl.Status -eq 400 -and $bl.Json.code -eq 'VALIDATION_ERROR') "HTTP $($bl.Status)"
$nt = Api-Get '/tickets/999999'
Check 'unknown ticket returns 404' ($nt.Status -eq 404)

# Database checks (optional, needs docker compose)
if (Get-Command docker -ErrorAction SilentlyContinue) {
    $cancelled = (Compose exec -T ticket-db psql -U bustrack -d ticketdb -tA -c "select count(*) from tickets where status='CANCELLED';") | Out-String
    Check 'declined purchases left CANCELLED tickets in ticket-db' ([int]($cancelled.Trim()) -ge 1) $cancelled

    $cards = (Compose exec -T payment-db psql -U bustrack -d paymentdb -tA -c "select max(length(card_last4)) from payments;") | Out-String
    Check 'payment-db stores only the last 4 card digits' ($cards.Trim() -eq '4') $cards
}

Show-Summary
