# IP Ban Security Tests

These scripts exercise the IP ban flow with simulated client IPs via `X-Forwarded-For`.
Because local requests come from loopback, `ClientIpResolver` trusts the header in dev.

## Smoke Test

Quick check without JMeter:

```powershell
powershell -ExecutionPolicy Bypass -File .\tests\security\ip-ban-smoke.ps1
```

Expected result: repeated invalid registration attempts trigger rate-limit escalation,
then `GET /api/home` from the same simulated IP returns `403` with `IP已被封禁`.

## JMeter Load Test

Run the JMeter plan:

```powershell
powershell -ExecutionPolicy Bypass -File .\tests\security\run-ip-ban-jmeter.ps1
```

Useful parameters:

```powershell
powershell -ExecutionPolicy Bypass -File .\tests\security\run-ip-ban-jmeter.ps1 `
  -BaseUrl "http://localhost:8080" `
  -RegisterLoops 35 `
  -HotspotThreads 8 `
  -HotspotLoops 45
```

If you have an admin JWT token, pass it to list and unblock the simulated abuse IP:

```powershell
powershell -ExecutionPolicy Bypass -File .\tests\security\run-ip-ban-jmeter.ps1 `
  -AdminToken "<admin-jwt-token>"
```

The runner writes `.jtl` and HTML reports under `tests/security/results/<timestamp>/`.

## What Is Covered

- Bulk registration abuse: invalid `POST /api/auth/register` requests exceed the IP rate-limit threshold and should lead to an IP ban.
- Banned IP enforcement: `GET /api/home` from the same simulated IP must return `403`.
- High-frequency API access: concurrent `GET /api/home` requests from another simulated IP exercise hotspot rate limiting and should return normal or `429` responses, not crash the service.
- Optional admin cleanup: when `-AdminToken` is provided, the JMeter plan calls `GET /api/admin/security/banned-ips` and `POST /api/admin/security/risk/unblock-ip`.
