# Round 62 evidence

Deliverable: `docker-images.yml` is green on develop. It had never been.

## Two independent defects, one behind the other

The workflow had failed on five consecutive develop runs before this round, all
reporting the same thing: `curl: (7) Failed to connect to localhost port 18080`.
One conclusion, two causes stacked behind each other.

### First: the API image cannot boot without a database

Flyway runs on startup and `ddl-auto` is `validate`, so with no PostgreSQL the
container exits and `/actuator/health` never answers. The smoke test was waiting
up to a minute for a server that could not exist.

Reproduced both halves locally with the real jar before changing anything:

```
no database    -> java.net.ConnectException: Connection refused: getsockopt
with database  -> GET /actuator/health  200  {"liveness":{"status":"UP"}}
```

The job now gets a `postgres:15-alpine` service with a health check.

### Second: nginx cannot start without its upstream

Once the API smoke passed, the web smoke failed - the same connection error, a
different container. nginx resolves `proxy_pass` upstreams **at startup**, not per
request, so with no host named `civic-api` it does not degrade the API route, it
refuses to boot:

```
nginx: [emerg] host not found in upstream "civic-api"
```

Reproduced locally with `nginx:1.27-alpine` before touching the workflow.

Both containers now share a network with a `civic-api` alias, which is also the
deployment topology: nginx in front, API behind.

## The check is now stronger than "the container starts"

The web smoke curls `/api/health` **through** nginx. That is the path a resident
takes - web, nginx, API, database - and a check limited to `/` cannot see whether
the proxy works at all, which is precisely the part that was broken.

The run log says so:

```
API container is up
Web container is serving
Web serves the app and proxies the API
```

## A missing variable that a laptop would never have shown

The API also refuses to boot without `JWT_SECRET`, which has no default on purpose.
The container got it from `/dev/urandom`, generated per run rather than hardcoded -
a literal secret in a workflow file is a literal secret that eventually ships.

This one was invisible locally because **a developer's shell usually already
exports `JWT_SECRET`**. I found that by checking my own environment after CI
reported `Could not resolve placeholder 'JWT_SECRET'`: it was there, 88 characters
long. So the local reproduction had passed partly by luck. Second time in this
round that a working machine hid a broken runner.

## Diagnostics, because the old failure said nothing

The old smoke test reported a connection error and nothing else, which cannot tell
a timeout from a crash from a wrong port - and the GitHub logs had already expired
(HTTP 410) by the time I went looking. Both smoke tests now print container logs when
the container exits early and again when the final check fails. Every cause above
was found by reading those logs, not by guessing.

## Three iterations of my own mistakes, all caught by CI in seconds

Worth recording because they are the kind of failure a local run cannot produce.

1. **A splice by line number dropped a step.** Reordering the smoke steps by index
   removed `Build Web image for smoke test` entirely. The YAML still parsed and every
   key I checked was present. CI failed with `pull access denied for
   local/open-civic-signal-os-web:smoke`. Validating a workflow by its keys says
   nothing about whether its steps are the right steps in the right order; reading
   the list of step names is what caught it.
2. **Reordering dropped a flag.** Moving to "start both, then test each" lost
   `--add-host=host.docker.internal:host-gateway` on the API container, and it came
   up with `UnknownHostException: host.docker.internal`.
3. **Before the last push I checked every flag of both containers in one pass** -
   network, alias, published ports, add-host, datasource URL, JWT_SECRET, and the two
   endpoints each smoke test curls. That check costs seconds and it is exactly what
   the previous three iterations were missing.

## Verification

| Check | Result |
| --- | --- |
| `docker-images.yml` on develop | **success** - all 23 steps, images published to GHCR |
| API smoke | passes |
| Web smoke, static + proxied API | passes |
| Full suite, `clean` | 446 run, 5 skipped |
| `npm run agent:preflight` | passed |
| OpenAPI parity | 178 / 147 / 9 - unchanged |

## Known limits

- **The proxy is asserted through `/api/health`, not through a real feature.** It proves routing and
  reachability, not that a specific screen's data loads.
- **The smoke test uses one postgres service on a fixed port.** GitHub runners are isolated so this is
  fine, but it does not run anywhere else.
- `JWT_SECRET` is generated per run, so the smoke image is never verified against a *particular* secret
  shape beyond "long enough to boot".
- The images are now published from every develop push, which is what the workflow already did; nothing
  about publishing changed.