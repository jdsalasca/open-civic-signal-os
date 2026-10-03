# Local Verification Path

Repeatable, in order. Every command here is the same one CI runs, plus the browser
evidence step that CI does not run.

## Prerequisites

- Node.js 20+ and npm (the frontend toolchain)
- JDK 21+ and Maven (the backend toolchain)
- Docker Engine (canonical runtime)

Check both toolchains before anything else. A missing tool silently downgrades
verification: `agent-preflight` skips the Java step when `mvn` is unavailable rather
than failing.

```bash
node -v && npm -v
mvn -v
docker version
```

## 1. Deterministic ranking reproduction

```bash
npm run prioritize
```

## 2. Full preflight

One command, run from the repository root:

```bash
npm run agent:preflight
```

This runs, in order:

| Step | Command | Fails on |
|---|---|---|
| Agent context | `agent:context:check` | missing mandatory agent docs |
| Backlog shape | `backlog:current:check` | missing sections or < 5 queued stories |
| Runtime secrets | `security:runtime:check` | literal secret or unpinned image in a tracked file |
| Frontend build | `build:web` | TypeScript or bundler error (only when web files changed) |
| Backend tests | `mvn -q test` | any failing test (only when Java files changed) |
| Contract ADR | `agent:adr:check` | contract changed without an ADR |
| Contract parity | `contract:parity:check` | an active controller route missing from the contract |

`AGENT_PREFLIGHT_STRICT=1` makes a missing Maven install a failure instead of a skip.

## 3. Backend test suite on its own

`mvn test` runs both `*Test` and `*IT` classes. This matters: Surefire's default
includes do not match `*IT`, and the repository's backend coverage lives in `*IT`
classes, so without the pinned includes in `apps/api-java/pom.xml` the suite silently
runs only the two unit classes.

```bash
cd apps/api-java && mvn test
```

## 4. Canonical runtime

```bash
cp infra/.env.example infra/.env   # then fill in POSTGRES_PASSWORD and JWT_SECRET
npm run docker:dev:up
npm run docker:dev:doctor
```

`POSTGRES_PASSWORD` and `JWT_SECRET` are required. Compose refuses to start without
them; that refusal is the intended behaviour, not a misconfiguration.

- frontend: <http://localhost:5173>
- API: <http://localhost:8081>
- mailpit: <http://localhost:8025>

## 5. Browser evidence

Not part of CI. Start the runtime first, then:

```bash
npm run agent:ux:pw -- open http://localhost:5173 --headed
npm run agent:ux:pw -- snapshot
npm run agent:ux:pw -- screenshot output/playwright/<name>.png
```

## 6. Playwright suite

Specs run against `BASE_URL`, defaulting to `http://localhost:3002`. Override it to
match whichever runtime you started:

```bash
cd apps/web-react
BASE_URL=http://127.0.0.1:5199 npx playwright test
```

Two suites need a live backend and will fail against a bare frontend:
`global-state-integrity.spec.ts` and `signal-detail-timeline.spec.ts` log in with a
seeded account. The rest mock their API routes and run against the frontend alone.

## Troubleshooting

**`agent:preflight` skips the Java step.** Set `AGENT_PREFLIGHT_STRICT=1`.

**`contract:parity:check` fails after adding an endpoint.** Document the route in
`packages/contracts/openapi.yaml`, or, for auth and local-only probes, add it to
`INTENTIONALLY_UNDOCUMENTED` in `scripts/check-openapi-parity.mjs` with a reason.

**`security:runtime:check` fails.** A tracked file carries a literal secret or an
unpinned image tag. Fix the file; do not widen the rule. See
`docs/architecture/ADR-20260321-runtime-secret-policy.md`.

**Port 5173 is already in use.** Another stack may hold it. Start Vite on a free port
and pass `BASE_URL` to Playwright:

```bash
npm --workspace apps/web-react run dev -- --port 5199 --strictPort
```