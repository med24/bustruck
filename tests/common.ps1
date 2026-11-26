# Shared helpers for the BusTrack test scripts (PowerShell 5.1+ and 7+)
$ErrorActionPreference = 'Continue'
Set-Location (Join-Path $PSScriptRoot '..')     # run everything from the bustrack/ root

$global:GW      = 'http://localhost:8080'
$global:API     = 'http://localhost:8080/api/v1'
$global:TICKET  = 'http://localhost:8081'
$global:TRACK   = 'http://localhost:8082'
$global:PAYMENT = 'http://localhost:8083'
$global:GRPC_HOST = 'host.docker.internal:9090'

if ($null -eq $global:TestPass) { $global:TestPass = 0; $global:TestFail = 0 }

Add-Type -AssemblyName System.Net.Http
if (-not $global:Http) {
    $global:Http = New-Object System.Net.Http.HttpClient
    $global:Http.Timeout = [TimeSpan]::FromSeconds(60)
}

function Write-Section($title) { Write-Host "`n=== $title ===" -ForegroundColor Cyan }

function Check($name, $ok, $detail = '') {
    if ($ok) {
        $global:TestPass++
        Write-Host "  [PASS] $name" -ForegroundColor Green
    } else {
        $global:TestFail++
        Write-Host "  [FAIL] $name" -ForegroundColor Red
        if ($detail) { Write-Host "         $detail" -ForegroundColor DarkYellow }
    }
}

function Show-Summary {
    $color = 'Green'; if ($global:TestFail -gt 0) { $color = 'Red' }
    Write-Host "`nResult: $($global:TestPass) passed, $($global:TestFail) failed" -ForegroundColor $color
}

# Generic HTTP call that never throws. Returns Status (0 = connection failure), Body, Json, Headers, Location
function Invoke-Http($Method, $Url, $Body = $null, $ContentType = 'application/json') {
    $result = [pscustomobject]@{ Status = 0; Body = ''; Json = $null; Headers = @{}; Location = $null }
    try {
        $req = New-Object System.Net.Http.HttpRequestMessage (New-Object System.Net.Http.HttpMethod($Method)), $Url
        if ($null -ne $Body) {
            $req.Content = New-Object System.Net.Http.StringContent($Body, [System.Text.Encoding]::UTF8, $ContentType)
        }
        $resp = $global:Http.SendAsync($req).GetAwaiter().GetResult()
        $result.Status = [int]$resp.StatusCode
        $result.Body = $resp.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        foreach ($h in $resp.Headers) { $result.Headers[$h.Key] = ($h.Value -join ',') }
        if ($resp.Headers.Location) { $result.Location = $resp.Headers.Location.ToString() }
        try { if ($result.Body) { $result.Json = $result.Body | ConvertFrom-Json } } catch { }
    } catch {
        $result.Body = $_.Exception.Message
    }
    return $result
}

function Api-Get($path)               { Invoke-Http 'GET'  "$($global:API)$path" }
function Api-Post($path, $obj = $null) {
    $json = $null
    if ($null -ne $obj) { $json = $obj | ConvertTo-Json -Compress }
    Invoke-Http 'POST' "$($global:API)$path" $json
}

function Buy($lineId, $card, $name = 'Test Passenger') {
    Api-Post '/tickets' @{ lineId = $lineId; passengerName = $name; cardNumber = $card }
}

function Wait-Until([scriptblock]$Condition, [int]$TimeoutSec = 60, [int]$IntervalSec = 2) {
    $end = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $end) {
        try { if (& $Condition) { return $true } } catch { }
        Start-Sleep -Seconds $IntervalSec
    }
    return $false
}

function Wait-Healthy($url, [int]$TimeoutSec = 90) {
    Wait-Until { (Invoke-Http 'GET' $url).Status -eq 200 } $TimeoutSec 2
}

# ---------- SOAP helpers ----------
function Send-Soap($inner) {
    $xml = '<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:pay="http://bustrack.com/payment"><soapenv:Body>' + $inner + '</soapenv:Body></soapenv:Envelope>'
    Invoke-Http 'POST' "$($global:PAYMENT)/ws" $xml 'text/xml'
}
function Soap-Authorize($ticketId, $amount, $card) {
    Send-Soap ("<pay:AuthorizePaymentRequest><pay:ticketId>$ticketId</pay:ticketId><pay:amount>$amount</pay:amount><pay:cardNumber>$card</pay:cardNumber></pay:AuthorizePaymentRequest>")
}
function Soap-Refund($paymentId)  { Send-Soap ("<pay:RefundPaymentRequest><pay:paymentId>$paymentId</pay:paymentId></pay:RefundPaymentRequest>") }
function Soap-Status($paymentId)  { Send-Soap ("<pay:GetPaymentStatusRequest><pay:paymentId>$paymentId</pay:paymentId></pay:GetPaymentStatusRequest>") }
function Get-XmlValue($xml, $tag) {
    $m = [regex]::Match($xml, "<(?:\w+:)?$tag>([^<]*)</")
    if ($m.Success) { return $m.Groups[1].Value } else { return $null }
}

# ---------- gRPC helper (grpcurl in a throw-away container, JSON sent via stdin to avoid quoting issues) ----------
function Invoke-Grpc($method, $jsonLines, [int]$MaxTime = 10) {
    $proto = (Resolve-Path 'contracts/proto').Path
    $args2 = @('run', '--rm', '-i', '-v', "${proto}:/proto", 'fullstorydev/grpcurl',
               '-plaintext', '-max-time', "$MaxTime", '-import-path', '/proto', '-proto', 'tracking.proto',
               '-d', '@', $global:GRPC_HOST, "bustrack.tracking.v1.TrackingService/$method")
    $out = $jsonLines | & docker @args2 2>&1 | ForEach-Object { "$_" }
    return ($out -join "`n")
}

function Compose { & docker compose @args }
