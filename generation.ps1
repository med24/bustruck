@'
function New-Service {
    [CmdletBinding()]
    param (
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Deps,
        [string]$GroupId = "com.bustrack",
        [string]$JavaVersion = "17"
    )

    $cleanPkg = "$GroupId." + ($Name -replace '-', '')
    $uri = [System.UriBuilder]::new("https", "start.spring.io", 443, "starter.zip")
    $uri.Query = "type=maven-project&language=java&javaVersion=$JavaVersion&groupId=$GroupId&artifactId=$Name&name=$Name&packageName=$cleanPkg&dependencies=$Deps"
    
    $zipPath = Join-Path $PWD "$Name.zip"

    try {
        Write-Host "Downloading from: $($uri.Uri.AbsoluteUri)" -ForegroundColor DarkGray
        Write-Host "Generating $Name..." -ForegroundColor Cyan
        
        $response = Invoke-WebRequest -Uri $uri.Uri.AbsoluteUri -OutFile $zipPath -UseBasicParsing -PassThru -ErrorAction Stop
        
        if ($response.Headers['Content-Type'] -notmatch 'zip|octet-stream') {
            throw "Server returned $($response.Headers['Content-Type']) instead of a ZIP."
        }

        Expand-Archive -Path $zipPath -DestinationPath $Name -Force
        Write-Host "Created $Name successfully.`n" -ForegroundColor Green
    }
    catch {
        Write-Error "Failed to create $Name : $_"
    }
    finally {
        if (Test-Path $zipPath) { Remove-Item $zipPath }
    }
}

$services = [ordered]@{
    "1" = @{ Name = "payment-service";  Deps = "web-services,data-jpa,postgresql,actuator" }
    "2" = @{ Name = "tracking-service"; Deps = "data-jpa,postgresql,actuator" }
    "3" = @{ Name = "ticket-service";   Deps = "web,validation,data-jpa,postgresql,actuator,web-services" }
    "4" = @{ Name = "api-gateway";      Deps = "cloud-gateway,actuator" }
}

Write-Host "=== BusTrack Microservice Generator ===" -ForegroundColor Yellow
foreach ($key in $services.Keys) {
    Write-Host "[$key] $($services[$key].Name)"
}
Write-Host "[A] Generate ALL services"
Write-Host "[C] Custom service`n"

$choice = Read-Host "Select an option (e.g., 1, 1,3, A, or C)"

if ($choice -match '^[Aa]$') {
    foreach ($svc in $services.Values) { New-Service -Name $svc.Name -Deps $svc.Deps }
}
elseif ($choice -match '^[Cc]$') {
    $customName = Read-Host "Enter service name (e.g., notification-service)"
    $customDeps = Read-Host "Enter comma-separated dependencies (e.g., web,actuator)"
    New-Service -Name $customName -Deps $customDeps
}
else {
    $selections = $choice -split ',' | ForEach-Object { $_.Trim() }
    foreach ($sel in $selections) {
        if ($services.Contains($sel)) { New-Service -Name $services[$sel].Name -Deps $services[$sel].Deps }
        else { Write-Warning "Invalid option '$sel' skipped." }
    }
}
'@ | Set-Content -Path .\generation.ps1 -Encoding UTF8

.\generation.ps1
