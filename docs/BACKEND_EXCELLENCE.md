# Backend Excellence

## Design rules

- Business logic in services, not controllers.
- Controllers map requests/responses only.
- Validation at boundaries.
- Explicit DTOs for API contracts.

## API quality

- Stable field naming.
- Version contracts intentionally.
- Preserve backward compatibility for existing clients.

## Operational quality

- Add health endpoint and readiness checks.
- Track key service metrics.
- Return meaningful status codes.

## Domain integrity

- Keep critical calculations deterministic.
- Document assumptions in decision logs.
- Include trace metadata where relevant.
