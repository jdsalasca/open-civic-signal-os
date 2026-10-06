# Round 92 evidence

Deliverable: `docker:dev:up` stops reporting a healthy stack it never verified. This was the sixth
false claim traced to trusting a message instead of a state, and this time the code was the source.

## The defect

```js
const healthTargets = [
  { name: "API", url: "http://localhost:8081/actuator/health" },
  { name: "Web", url: "http://localhost:5173" }
];
const requiredHostPorts = [5173, 8081, 5432, 8025];
```

Round 90 parameterised the compose ports. This file kept the old literals, so once ports were
overridden the readiness probe went somewhere else entirely - on this machine `8081` is the prod
civic API and `5173` is `open-university-os-frontend`. Both answer. So:

```
Docker dev stack is healthy.
```

was printed immediately, in about a second, while the dev API was still downloading Maven artifacts
and compiling 420 Java files. Round 90 recorded that message as evidence and I recorded it as the
reason to believe the stack was fine. Both wrong, and the cause was a hardcoded literal in a script
whose compose file it was supposed to mirror.

## The fix

The ports now come from the same environment variables the compose file reads, with the same
defaults:

```js
const devPorts = {
  api: process.env.API_PORT ?? "8081",
  web: process.env.WEB_PORT ?? "5173",
  db: process.env.POSTGRES_PORT ?? "5432",
  mail: process.env.MAILPIT_PORT ?? "8025",
};
```

One place defines the dev ports for both the compose file and the tooling, so they cannot drift
again. `requiredHostPorts` derives from the same object, which keeps `doctor` honest too.

## Verification, behavioural

Round 90's run with overridden ports printed `Docker dev stack is healthy.` in seconds. The same run
after the fix, same machine, same state:

```
Container infra-civic-db-dev-1 Healthy
Docker dev stack did not become healthy in time.
```

The probe now reports the truth because it is talking to the dev API rather than to whatever else
answers on 8081. `docker-dev-up-overridden.txt` is the raw output.

## Also confirmed: round 91's healthcheck is live

```
docker inspect infra-civic-api-dev-1 -> health=starting | restarts=0
```

Previously `health=NINGUNO`. The healthcheck added last round took effect on recreate, and the service
reports a status instead of nothing.

## Still pending

The container has finished the main compile - the logs moved on to `Compiling 79 source files ...
target/test-classes` - and is not yet listening, so the seeded `admin` login remains unobserved.
Round 89's diagnosis is unchanged; this round made the tooling stop lying about it, which is what
would otherwise have kept misleading the next reader.

## Known limitations

- Dev and prod stacks are both still running on this machine. The prod stack was never touched.
- The gate is unchanged at 20 specs, 94 passed, retries disabled.