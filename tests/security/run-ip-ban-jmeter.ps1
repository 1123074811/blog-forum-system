param(
  [string]$BaseUrl = "http://localhost:8080",
  [string]$TestIp = "",
  [string]$HotspotTestIp = "",
  [string]$AdminToken = "",
  [string]$JMeter = "jmeter",
  [int]$RegisterLoops = 35,
  [int]$RegisterThreads = 1,
  [int]$HotspotLoops = 45,
  [int]$HotspotThreads = 8
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$plan = Join-Path $PSScriptRoot "ip-ban-load-test.jmx"
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$resultDir = Join-Path $PSScriptRoot "results\$timestamp"
$reportDir = Join-Path $resultDir "html-report"
$jtl = Join-Path $resultDir "ip-ban-load-test.jtl"

New-Item -ItemType Directory -Force -Path $resultDir | Out-Null

if (-not $TestIp) {
  $TestIp = "198.51.100.$(Get-Random -Minimum 10 -Maximum 230)"
}
if (-not $HotspotTestIp) {
  $HotspotTestIp = "198.51.100.$(Get-Random -Minimum 231 -Maximum 250)"
}

$uri = [Uri]$BaseUrl
$protocol = $uri.Scheme
$hostName = $uri.Host
$port = $uri.Port
if ($port -lt 0) {
  $port = if ($protocol -eq "https") { 443 } else { 80 }
}

Write-Host "BaseUrl:        $BaseUrl"
Write-Host "Abuse Test IP:  $TestIp"
Write-Host "Hotspot IP:     $HotspotTestIp"
Write-Host "JMeter plan:    $plan"
Write-Host "Results:        $resultDir"

$argsList = @(
  "-n",
  "-t", $plan,
  "-l", $jtl,
  "-e",
  "-o", $reportDir,
  "-JPROTOCOL=$protocol",
  "-JHOST=$hostName",
  "-JPORT=$port",
  "-JTEST_IP=$TestIp",
  "-JHOTSPOT_TEST_IP=$HotspotTestIp",
  "-JREGISTER_LOOPS=$RegisterLoops",
  "-JREGISTER_THREADS=$RegisterThreads",
  "-JHOTSPOT_LOOPS=$HotspotLoops",
  "-JHOTSPOT_THREADS=$HotspotThreads"
)

if ($AdminToken) {
  $argsList += "-JADMIN_TOKEN=$AdminToken"
}

& $JMeter @argsList
$exitCode = $LASTEXITCODE

Write-Host ""
Write-Host "JTL:         $jtl"
Write-Host "HTML report: $reportDir\index.html"
Write-Host "Exit code:   $exitCode"

if (-not $AdminToken) {
  Write-Host ""
  Write-Host "No AdminToken was provided, so the simulated abuse IP may remain banned until its TTL expires."
  Write-Host "To clean up immediately, run the same command with -AdminToken '<admin jwt token>'."
}

exit $exitCode
