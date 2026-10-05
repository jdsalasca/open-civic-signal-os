# Round 67 evidence

Deliverable: three Playwright specs that had never run anywhere now run, and the diagnosis that
explained the 26 red specs is corrected.

## Round 67 as planned was not worth doing

The plan said the topbar fallback still announces unknown routes as "Home". I checked whether that
code can be reached before changing it: `NotFound` and `Unauthorized` both render `<Layout authMode>`,
and `Layout` returns early on `authMode`, so **neither ever paints a topbar**. Every route that does
paint one matches a nav item exactly, matches the `/communities/` prefix, or hits the detail map added
in round 66.

The fallback is unreachable. Renaming it would have been a round spent on dead code, so it was dropped
in favour of something real.

## No workflow runs this suite

`frontend-ux-evidence-gate.yml` only checks that a PR body contains evidence. Nothing executes the 46
spec files. So "26 failing" never meant anything: those specs had simply never run, in CI or locally.

## What was actually wrong, and it was not what rounds 65 and 66 recorded

Rounds 65 and 66 attributed the failures to specs logging in against a backend with no `admin`
account. **That was wrong for the subset checked this round.** `dashboard-filter-query` and
`dashboard-aging` mock their own API routes and never log in.

Instrumenting the run gave the answer:

```
200 GET /api/signals/prioritized?page=0&size=10
200 GET /api/signals/meta
200 GET /api/notifications/recent
200 GET /api/signals/duplicates
401 GET /api/help-center?surface=DASHBOARD&lang=en
401 GET /api/signals/aging
403 POST /api/auth/refresh        -> logout() -> /login
```

Two routes nobody remembers to mock. The request reaches the live backend, 401s, the refresh attempt
returns 403, and `axios.ts` calls `logout()`, which wipes the seeded session. **The symptom points at
the view, not at the gap**: the dashboard renders, `dashboard-hero` appears, and then every later
assertion fails on a login page nobody was looking at.

`auth/me` is the trap that makes this hard to see. `App.tsx` wraps it in a try/catch and keeps the
local preference, so it looks handled - but the axios response interceptor runs before the catch.

## A fixture bug that looked like a performance defect

After mocking the two routes the spec still failed, so the request loop was measured:

```
signals/aging requested 93 times in 8 seconds, ~12 per second, sustained
```

That is a serious-looking defect, and the first three hypotheses were wrong: the effect dependencies
are stable primitives, `activeCommunityId` never changes, and the stack trace was cut by the async
boundary. Tagging the hero DOM node settled it - the node was replaced, so the component was
**remounting**, not re-rendering. Console output gave the cause:

```
TypeError: Cannot read properties of undefined (reading 'length')
The above error occurred in the <Dashboard> component
```

`Dashboard.tsx:762` reads `aging.atRiskSignals.length`. Both `atRiskSignals` and `trend` are required
in `SignalAging`, so **the offending payload was my own invalid fixture**, not a product bug. React
unmounts the crashed tree, the boundary retries, it crashes again, and each attempt re-requests aging.

Correcting the payload to satisfy the type removed the loop: the two filter tests went from 30s
timeouts to passing in **5.4s**.

What survives from this is worth recording: a render crash presents as sustained network traffic
rather than as an error, and nothing in the app caps that. Fixing the boundary is a separate decision.

## What changed

Three specs are live again, and the pattern is now reusable:

- `helpers/session.ts` - `seedSession`, `mockAppBootstrap`, `seedAuthenticatedApp`. Absorbs the local
  `seedSession` copy-pasted into four other specs.
- `helpers/dashboard.ts` - `mockDashboardRoutes`, answering the two routes nobody remembers. It
  deliberately does **not** answer `signals/prioritized` or `signals/meta`: those are each spec's own
  data, and Playwright checks the most recently added route first, so a shared default registered
  later would silently override a spec's assertions on the query it is trying to observe.
- `signalDetailFixture` now reuses the shared bootstrap instead of duplicating it.

No production code was touched this round, so the evidence is the real test output rather than a
screenshot. `playwright-run.txt` is that run, verbatim.

## Red before green

```
dashboard-filter-query   2 failed (30s timeouts on a click that could not stabilise)
dashboard-clarity        1 failed (dashboard-hero never rendered)
after                    3 passed (8.8s)
```

`signal-detail-lifecycle`, `signal-detail-describes-itself`, `no-locale-timestamps` and
`public-backlog` were run alongside: `18 passed`.

The refactor also broke my own round-66 test and the suite caught it: collapsing
`seedSession(page, 'CITIZEN')` into the shared helper silently dropped the citizen role and the
dashboard routes, so the dashboard framing assertion started failing. Restored explicitly.

## Known limitations

- `dashboard-aging` still fails, and it is not in this round. It registers its own aging payload, so
  it never touched the shared helper, and it has a separate problem of its own.
- `dashboard-guided-home`, `weekly-digest`, `merge-review`, `field-data-mode` and
  `community-rooms-history` still need work. The first three need real login; `guided-home` needs
  seeded memberships and a role switch. Round 68.
- The "26 red specs" figure is now known to be a mixture of at least three different causes. It should
  not be cited as a single number until each is re-measured.
- The local Vite server on 5299 died three times during this round and had to be restarted. Unrelated
  to these defects, but it makes long Playwright runs flaky in a way worth understanding.