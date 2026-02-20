# Observability

## Logs

- Structured logs with request identifiers.
- Include operation name and result status.
- Avoid PII in logs.

## Metrics

- Throughput (requests/jobs per period).
- Error rate by endpoint and operation.
- Latency distribution for critical paths.
- Domain KPIs for product trust and impact.
- Prioritized feed custom metrics:
  - `signalos.prioritized.latency`
  - `signalos.prioritized.requests.total`
  - `signalos.prioritized.requests.errors.total`
  - `signalos.prioritized.result.size`

## Alerts

- Error-rate threshold alerts.
- Stale data alerts.
- Pipeline failure alerts.

## Dashboards

- Service health dashboard.
- Domain impact dashboard.
- Risk and anomaly dashboard.
