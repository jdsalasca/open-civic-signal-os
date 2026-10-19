# Round 93 evidence

Deliverable: the dev API healthcheck window is sized to the build it actually has to wait for,
instead of declaring `unhealthy` while the container is still compiling.

## What round 91's healthcheck did when it was finally applied

```
docker inspect infra-civic-api-dev-1 -> health=unhealthy
docker logs -> Building Open Civic Signal OS API 0.1.0
            -> Compiling 79 source files ... to target/test-classes
```

The service was serving nothing and the healthcheck said so. That is the change working: before it,
the service reported `NINGUNO` and the tooling printed "Docker dev stack is healthy".

But the report is wrong for a different reason, and that is this round's finding. The container was
not broken, it was mid-build, and the window I gave it expired during a legitimate cold compile.

## Not a misconfiguration

Worth ruling out explicitly, because "it compiles test sources on boot" would have been the obvious
suspicion:

```
infra/scripts/dev-api-entrypoint.sh
  mvn -B -q -DskipTests dependency:go-offline
  mvn -B -DskipTests -Dspring-boot.run.profiles=dev spring-boot:run
```

`-DskipTests` is set. The `test-classes` line is `spring-boot:run` driving the lifecycle, not a test
suite running at boot. Nothing is misconfigured; the first boot on a cold `civic-m2-cache` downloads
the world and compiles 420 sources, and that takes longer than five minutes.

## The fix

Round 91 chose `start_period: 180s` with `retries: 30`, reasoning that dev needed more headroom than
prod. Measured against the real cold build, that was still not enough, so it produced a false
`unhealthy` - the same class of lie as the "healthy" it replaced, just pointing the other way.

```yaml
interval: 10s   timeout: 5s   retries: 120   start_period: 600s
```

That is roughly twenty minutes of grace before the check can fail, which matches what a cold Maven
build on a fresh cache actually costs. Once the cache is warm this window is never approached.

## Verification

```
docker compose -f infra/docker-compose.dev.yml config --quiet   -> config OK
docker compose ... config -> retries: 120   start_period: 10m0s
```

The running container still reports `health=unhealthy` because the window only applies on recreate.
That is the same limitation as round 91 and is not worked around here: recreating to re-verify would
restart the build the wider window exists to accommodate.

## The loop that closes here

Three rounds of chasing "no seeded admin" produced: the real cause (prod profile, round 89), the
ability to act on it (parameterised ports, round 90), a healthcheck that reports honestly instead of
silently (91), tooling that no longer lies about readiness (92), and now a window that stops
reporting honestly-unhealthy mid-build. The account itself is still unobserved because the build has
not finished.

The shape of this is worth recording: every one of those rounds was me trusting a status string. The
status strings were the bug.

## Known limitations

- Dev and prod stacks both still running on this machine; prod untouched.
- The seeded `admin` remains unobserved and no backend spec has run end to end.
- The gate is unchanged at 20 specs, 94 passed, retries disabled.