# ADR-20260305 Interface Mode Profile Contract

## Status

Accepted

## Context

`story:OCS-P1-032` requires the product to support calmer simple-mode UX for first-time community members while preserving a denser advanced mode for coordinators, moderators, and power users. The preference must survive sessions and be portable beyond one browser instance.

The existing profile contract already carries user-facing identity preferences. Reusing that contract keeps the interface mode backend-owned instead of letting the frontend invent behavior without an auditable source of truth.

## Decision

Add `interfaceMode` to the authenticated profile contract and persist it on `users`.

- Domain enum: `SIMPLE | ADVANCED`
- `GET /api/auth/profile/me` returns `interfaceMode`
- `PUT /api/auth/profile/me` accepts `interfaceMode`
- public profile responses also include `interfaceMode` for contract stability
- frontend settings store maps the backend enum to local shell classes and view simplification behavior

## Consequences

Positive:

- simple vs advanced mode becomes durable and reproducible
- frontend shell behavior can be traced back to a persisted preference
- future device/server sync has a clear contract anchor

Trade-offs:

- profile contract expands beyond identity/privacy fields
- layout and dashboard now depend on a persisted experience preference

Follow-up:

- extend the mode across more views beyond shell and dashboard
- consider role-based defaulting rules on first login for institutional users
