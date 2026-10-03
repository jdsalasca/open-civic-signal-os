# ADR-20260321: Contract Parity Gate And Verification Path Contract

- Status: Accepted
- Date: 2026-03-21
- Closes: top-20 backlog items 17 (`P1 developer-experience`) and 18 (`P1 trust-critical: align remaining active OpenAPI/examples`)
- Story: `BE-10` platform hardening pack

## Context

Two of the four remaining BE-10 items are "the contract is not true" problems, and both were unfalsifiable at review time.

Nothing compared the controller route surface against `packages/contracts/openapi.yaml`. A route could be added, shipped, and documented nowhere, and no gate would notice. That is not hypothetical: writing this checker immediately found `DELETE /api/community/activities/{activityId}`, which the OCS-P1-044 change had introduced and forgotten, plus `POST /api/notifications/relay/top-10` and three auth routes that had never been described.

Nothing documented how to verify the project. `agent:preflight` is the gate, but a new contributor has to read `scripts/agent-preflight.mjs` to learn that the Java step is skipped when Maven is absent, that `mvn test` only runs `*IT` because of a pinned Surefire include, or which Playwright suites need a live backend. Those are exactly the details that decide whether a verification claim is true.

## Decision

Add `scripts/check-openapi-parity.mjs` and make the verification path a document.

The parity checker:

- extracts `@RequestMapping` base paths and `@Get/Post/Put/Patch/DeleteMapping` routes from every tracked file under the controller package
- normalises `{param}` placeholders so `/activities/{activityId}` matches the spec shape
- compares against the `paths:` block of the OpenAPI document, checking both the path and the verb
- allows an explicit `INTENTIONALLY_UNDOCUMENTED` map, so an omission needs a stated reason rather than being invisible
- **guards against its own silent pass**: it fails if zero controllers are found or zero routes are extracted, because a broken path glob would otherwise report success while checking nothing

It runs inside `agent:preflight` only when the contract changed, alongside the existing ADR gate.

Nine routes are intentionally undocumented, all with reasons: the eight `/api/auth/**` handshake endpoints, and `/api/test/email`, a local-only probe.

Two genuinely public routes were documented in the same change rather than excused: the activity cancellation I had missed, and the top-10 relay.

`docs/LOCAL_VERIFICATION.md` records the ordered path: toolchain checks, `npm run prioritize`, `npm run agent:preflight` with a table of what each step fails on, the Surefire `*IT` explanation, `npm run docker:dev:up` with the required secrets, browser evidence, the `BASE_URL` override for Playwright, which two suites need a live backend, and troubleshooting for each gate.

## Consequences

Positive:

- a new endpoint without a contract entry fails the preflight instead of being discovered later
- adding an omission to the allowlist is a visible, reviewable act with a reason attached
- the checker cannot report success while checking nothing
- a contributor can reproduce the full verification path from one document

Trade-offs:

- route extraction is regex over source, not a Spring introspection dump. It reads mapping annotations only; a programmatically registered route would be invisible. Everything in this repository uses annotations, and the zero-route guard catches the case where the whole approach stops working.
- `git ls-files` on the controller directory also returns the `dto/` subdirectory, so the reported controller count includes DTO records. Harmless, since a record carries no mapping annotation, but the number is not "10 controllers". Fixing it means globbing `web/*.java`, which I left alone to keep the checker's surface minimal.
- the parity gate runs only when `packages/contracts/openapi.yaml` changed. A change to a controller alone does not trigger it, which is a real gap: adding an endpoint without touching the contract would pass preflight. The correct trigger is "controller files changed OR contract changed", which is a one-line change to `agent-preflight.mjs` and the honest next step.
- path comparison ignores query parameters and request bodies, so a route can be documented with the wrong payload and pass.
- the document is prose and drifts. It names the gate commands and the traps, which are stable; exact script output is not reproduced.

## Validation

- checker fails against the pre-change tree with 5 findings: the missed activity cancellation, the top-10 relay, and three auth routes
- checker passes after documenting the two public routes and adding the auth reasons
- checker correctly fails when a probe controller with an undocumented route is staged, naming file and line
- `node scripts/check-openapi-parity.mjs` reports 123 routes across the controller package, 98 documented paths, 9 intentionally undocumented
- `npm run agent:preflight` runs the new gate and passes
- OpenAPI re-parsed with `js-yaml` after both additions