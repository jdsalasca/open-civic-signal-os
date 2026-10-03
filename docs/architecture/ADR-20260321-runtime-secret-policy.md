# ADR-20260321: Runtime Secret Policy Contract

- Status: Accepted
- Date: 2026-03-21
- Closes: top-20 backlog item 16 (`P0 security: hardened compose/runtime secret policy`)
- Story: `BE-10` platform hardening pack

## Context

The repository shipped with live-looking secrets committed to git and hardcoded database credentials in every compose file:

- `docker-compose.yml:31` — `JWT_SECRET: "strategic-civic-intelligence-key-secure-2026-v1"`
- `infra/.env.example:1` — `JWT_SECRET=404E6352...` (a 64-character hex string)
- `POSTGRES_PASSWORD: password` and `SPRING_DATASOURCE_PASSWORD: password` in `docker-compose.yml`, `infra/docker-compose.yml`, and `infra/docker-compose.dev.yml`
- `.gitignore` did not list `.env` at all, so the local `.env` files holding a real SMTP password and a Google credentials path were untracked only by accident and a single `git add .` would have committed them
- `axllent/mailpit:latest` unpinned in two compose files

`JWT_SECRET` was already required in `infra/docker-compose.yml` via `${JWT_SECRET:?...}`, which proved the pattern worked and had simply never been applied consistently. A hardcoded JWT signing key means anyone with repository access can mint valid tokens; a hardcoded database password means the database is reachable by anyone who guesses `password`.

The SMTP password from the untracked `.env` was confirmed absent from git history. The two JWT secrets are present in history and remain there; rotating them is an operational action outside this repository.

## Decision

Make every runtime secret an explicit, required environment variable, and enforce it with a gate:

- `POSTGRES_PASSWORD` is `${POSTGRES_PASSWORD:?...}` in both `docker-compose.yml` and `infra/docker-compose.yml`, so compose refuses to start without it. The dev override keeps a value but names it `local-dev-only-change-me` so it can never be mistaken for a deployable default.
- `JWT_SECRET` is `${JWT_SECRET:?...}` in the root `docker-compose.yml`, which is the only remaining file that hardcoded it.
- `CORS_ALLOWED_ORIGINS` is env-driven in both files.
- `infra/.env.example` carries placeholders only, plus a command to generate a real secret.
- `.gitignore` now lists `.env`, `.env.*`, and `infra/secrets/`, while explicitly un-ignoring the two tracked `.env.example` files.
- `axllent/mailpit` is pinned to `v1.31` instead of `latest`.
- `scripts/check-runtime-secrets.mjs` fails when a tracked file contains a literal `JWT_SECRET`, a literal `POSTGRES_PASSWORD` or `SMTP_PASSWORD`, an unpinned mutable image tag, or a tracked non-example `.env`. It runs inside `agent:preflight`, so the debt cannot return unnoticed.

`POSTGRES_DB` and `POSTGRES_USER` keep defaults. They are not secrets, and requiring them would add friction without adding safety.

## Consequences

Positive:

- compose refuses to start in an environment where the database password or JWT signing key is not supplied
- a committed `.env`, a literal secret, or an unpinned image fails the gate before merge
- the example file is safe to read because it contains nothing real

Trade-offs:

- **existing deployments must now supply `POSTGRES_PASSWORD` and `CORS_ALLOWED_ORIGINS`.** This is a deliberate breaking change to the runtime contract; anyone with a scripted `docker compose up` that relied on the literal `password` will get a clear compose error rather than a silent insecure default.
- the JWT secrets already in git history are not removed. Rewriting published history is a separate, disruptive operation and rotation is an operational task for the operator. The gate prevents new occurrences; it does not clean old ones.
- the secret patterns are heuristics. They catch the shapes used in this repository, not arbitrary credential formats. A novel secret format would need a rule added.
- the gate reads only tracked files, so a secret present solely in an untracked local file is out of scope by design; that is what `.gitignore` is for.

## Validation

- `scripts/check-runtime-secrets.mjs` fails against the pre-change tree, reporting both unpinned `mailpit:latest` tags, and passes after the pin
- `node scripts/check-runtime-secrets.mjs` output verified in both directions
- wired into `scripts/agent-preflight.mjs` as `npm run security:runtime:check`
- `infra/.env.example` and both compose files reviewed for remaining literals; none remain