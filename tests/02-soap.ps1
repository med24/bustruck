. "$PSScriptRoot\common.ps1"
Write-Section '02 - SOAP: payment-service (called directly)'

# Authorize OK
$ok = Soap-Authorize 9001 2.50 4111111111111111
Check 'AuthorizePayment approved' ($ok.Status -eq 200 -and $ok.Body -match 'APPROVED') "HTTP $($ok.Status) $($ok.Body)"
$paymentId = Get-XmlValue $ok.Body 'paymentId'
Check 'response contains a paymentId' ([bool]$paymentId)

# Declined card -> SOAP Fault
$dec = Soap-Authorize 9002 2.50 4000000000000002
Check 'declined card returns SOAP Fault CARD_DECLINED' ($dec.Status -eq 500 -and $dec.Body -match 'CARD_DECLINED' -and $dec.Body -match 'Insufficient funds') $dec.Body

# Invalid card
$inv = Soap-Authorize 9003 2.50 12345
Check 'invalid card returns SOAP Fault INVALID_CARD' ($inv.Status -eq 500 -and $inv.Body -match 'INVALID_CARD') $inv.Body

# Limit exceeded
$lim = Soap-Authorize 9004 5000 4111111111111111
Check 'amount over limit returns CARD_DECLINED' ($lim.Status -eq 500 -and $lim.Body -match 'CARD_DECLINED' -and $lim.Body -match 'Limit exceeded') $lim.Body

# GetPaymentStatus
$st = Soap-Status $paymentId
Check 'GetPaymentStatus returns APPROVED' ($st.Status -eq 200 -and $st.Body -match 'APPROVED' -and (Get-XmlValue $st.Body 'ticketId') -eq '9001') $st.Body

# Refund
$rf = Soap-Refund $paymentId
Check 'RefundPayment returns REFUNDED' ($rf.Status -eq 200 -and $rf.Body -match 'REFUNDED') $rf.Body
$rf2 = Soap-Refund $paymentId
Check 'second refund is refused (INVALID_PAYMENT_STATE)' ($rf2.Status -eq 500 -and $rf2.Body -match 'INVALID_PAYMENT_STATE') $rf2.Body

# Unknown payment
$nf = Soap-Status 99999999
Check 'unknown payment returns PAYMENT_NOT_FOUND' ($nf.Status -eq 500 -and $nf.Body -match 'PAYMENT_NOT_FOUND') $nf.Body

Show-Summary
