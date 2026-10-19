# Round 91 evidence

Deliverable: `civic-api-dev` gets the healthcheck it never had, so "Docker dev stack is healthy"
stops being a claim nothing verifies.

## What the API was actually doing

Round 90 reported the dev API as never becoming healthy. It was not broken:

```
docker inspect infra-civic-api-dev-1 -> running | restarts=0 | healthcheck=NINGUNO
docker logs -> Downloading ... spring-tx-6.1.11.jar (282 kB)
           -> --- compiler:3.13.0:compile @ signalos-api ---
           -> Compiling 420 source files with javac [debug parameters release 21]
```

A cold Maven cache inside the container, downloading dependencies and compiling 420 Java files. Slow,
not failed. Nothing was wrong with the service; there was simply no way to tell healthy from still
compiling, because the service declares no healthcheck at all.

## The defect

`infra/docker-compose.yml` healthchecks the API:

```yaml
healthcheck:
  test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
  interval: 10s
  timeout: 5s
  retries: 5
```

`infra/docker-compose.dev.yml` did not. So `civic-db-dev` and `civic-mail-dev` report health and
`civic-api-dev` reports nothing, and the `docker:dev:up` success message - "Docker dev stack is
healthy" - had nothing behind it for the one service whose readiness matters most. That message is
what round 90 trusted, and it was wrong to.

## The fix

Same endpoint, same shape as the prod compose, with a `start_period` that accommodates a cold Maven
build:

```yaml
healthcheck:
  test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
  interval: 10s
  timeout: 5s
  retries: 30
  start_period: 180s
```

`retries: 30` and `start_period: 180s` rather than prod's `retries: 5`, because the dev container
compiles 420 sources on first boot and prod's values would mark it unhealthy before it finishes.

## Verification

Config level, which is what is claimed:

```
docker compose -f infra/docker-compose.dev.yml config --quiet   -> config OK
docker compose ... config  -> test: curl -f http://localhost:8080/actuator/health || exit 1
                          -> start_period: 3m0s
```

## Not verified, stated plainly

The running container still reports `health=NINGUNO`, because a healthcheck is only applied when the
service is recreated. Recreating it would kill the in-progress compile to prove a config change that
`docker compose config` already shows is correct. So the runtime half of this verification is
pending, and the seeded `admin` account remains unobserved. Round 89's diagnosis is unchanged.

## Known limitations

- Dev and prod stacks are both still running on this machine; the prod stack was never touched.
- `scripts/docker-dev.mjs` still prints hardcoded `localhost:8081` and `localhost:5173`, which
  misleads whenever a port is overridden. That is the next item and is one line.
- The gate is unchanged at 20 specs, 94 passed, retries disabled.