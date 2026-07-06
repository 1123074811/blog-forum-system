param(
  [string]$BaseUrl = "http://localhost:8080",
  [string]$TestIp = "",
  [int]$Requests = 35,
  [int]$DelayMs = 50
)

$ErrorActionPreference = "Stop"

if (-not $TestIp) {
  $TestIp = "198.51.100.$(Get-Random -Minimum 10 -Maximum 230)"
}

$headers = @{
  "Content-Type" = "application/json"
  "X-Forwarded-For" = $TestIp
}

Write-Host "BaseUrl: $BaseUrl"
Write-Host "Test IP: $TestIp"
Write-Host "Sending $Requests invalid register requests to trigger rate-limit based IP ban..."

for ($i = 1; $i -le $Requests; $i++) {
  $body = @{
    username = "abuse_$i"
    password = "Aa123456"
    email = "abuse_$i@example.test"
    captchaId = "invalid"
    captchaCode = "0000"
    emailCode = "000000"
  } | ConvertTo-Json -Compress

  try {
    $response = Invoke-WebRequest -Uri "$BaseUrl/api/auth/register" -Method Post -Headers $headers -Body $body -UseBasicParsing -TimeoutSec 10
    Write-Host ("{0,2}. HTTP {1}" -f $i, [int]$response.StatusCode)
  } catch {
    $status = $_.Exception.Response.StatusCode.value__
    Write-Host ("{0,2}. HTTP {1}" -f $i, $status)
  }
  Start-Sleep -Milliseconds $DelayMs
}

Write-Host ""
Write-Host "Verifying that the same simulated IP is now blocked..."
try {
  $verify = Invoke-WebRequest -Uri "$BaseUrl/api/home" -Method Get -Headers @{ "X-Forwarded-For" = $TestIp } -UseBasicParsing -TimeoutSec 10
  Write-Host "Verification HTTP $([int]$verify.StatusCode)"
  Write-Host $verify.Content
  exit 1
} catch {
  $status = $_.Exception.Response.StatusCode.value__
  $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
  $content = $reader.ReadToEnd()
  Write-Host "Verification HTTP $status"
  Write-Host $content
  if ($status -eq 403) {
    Write-Host "PASS: IP ban filter blocked the simulated abuse IP."
    exit 0
  }
  exit 1
}
