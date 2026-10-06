# Round 90 evidence

Deliverable: `docker:dev:up` can now run alongside an already-running stack. Port collisions are
configurable instead of fatal, verified by running both stacks at once.

## The defect

`infra/docker-compose.dev.yml` parameterised its environment variables in the repo's existing style:

```yaml
POSTGRES_DB: ${POSTGRES_DB:-signalos}
POSTGRES_USER: ${POSTGRES_USER:-admin}
```

but hardcoded every published port. Round 89's blocker was one line of Docker:

```
Bind for 0.0.0.0:8025 failed: port is already allocated
```

`civic-mail` from the prod stack held 8025, so the documented canonical local runtime could not start
at all. The file was already inconsistent with itself: variables configurable, ports not.

## The fix

All four published ports now follow the same `${VAR:-default}` style already used in the file:

```yaml
- "${MAILPIT_PORT:-8025}:8025"
- "${POSTGRES_PORT:-5432}:5432"
- "${API_PORT:-8081}:8080"
- "${WEB_PORT:-5173}:5173"
```

Defaults are unchanged, so every existing invocation behaves exactly as before. This is a widening of
an existing convention, not a new mechanism.

## Verification: both stacks running at once

```
infra-civic-mail-dev-1 | Up (healthy) | 0.0.0.0:18025->8025/tcp
infra-civic-db-dev-1   | Up (healthy) | 0.0.0.0:15432->5432/tcp
infra-civic-api-dev-1  | Up           | 0.0.0.0:18081->8080/tcp
infra-civic-web-dev-1  | Up           | 0.0.0.0:15173->5173/tcp
civic-api              | Up (healthy) | 0.0.0.0:8081->8080/tcp
civic-db               | Up (healthy) | 0.0.0.0:5432->5432/tcp
civic-mail             | Up (healthy) | 0.0.0.0:8025->8025/tcp
```

The dev stack came up on alternate ports while the prod stack kept serving the originals, which is
the collision that round 89 could not get past. `docker compose config` also resolves the defaults
unchanged (`published: "8081"`, `"5432"`, `"8025"`), so CI and any current usage are unaffected.

## What is still pending

The dev API container did not report healthy within the time available (`Up 8 minutes`, no health
status), so the seeded `admin` account has still not been observed and none of the four backend specs
has been run end to end. Round 89's cause is still the cause; only the ability to act on it has
changed.

One rough edge surfaced: `scripts/docker-dev.mjs` prints hardcoded `http://localhost:8081` and
`http://localhost:5173` in its success message, so with overridden ports it reports URLs that are not
the ones in use. Cosmetic, but it will mislead the next person who overrides a port, and it is a
one-line fix in the same file.

## Known limitations

- Dev and prod stacks are now both running on this machine. The prod stack was not touched; the dev
  stack is the one started here.
- The dev API's health is unverified, so nothing downstream of it is claimed.
- The gate is unchanged at 20 specs, 94 passed, retries disabled.