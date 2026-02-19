# Security Baseline

## Data

- Minimize personal data collection.
- Avoid storing sensitive raw payloads unless required.
- Sanitize logs and exports.

## API

- Validate all input.
- Use strict schema for request payloads.
- Return generic errors for sensitive failures.

## Dependency hygiene

- Review dependency updates regularly.
- Avoid unnecessary packages.
- Keep CI checks for vulnerabilities.

## Governance

- Document threat assumptions.
- Track security-impacting decisions in docs.
