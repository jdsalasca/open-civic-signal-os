# Round 70 evidence

Deliverable: a CI workflow that actually runs the Playwright suite, and the measurement that
decides how much of it the gate covers today.

## The suite had never run anywhere

`frontend-ux-evidence-gate.yml` checks that a PR body contains evidence. It never executes a test.
That is the root cause behind rounds 67 to 69: specs that had never passed anywhere could sit
undetected, and a broken assertion in a view could stay unnoticed for as long as it stayed unrun.

`playwright.config.ts` already encoded the intended CI posture - one worker, two retries,
`forbidOnly` - and nothing used it.

## The dev server was the wrong thing to measure against

Round 69 left an open question: the suite lost 2 to 4 tests to timeouts when more spec files shared
one dev server. Before writing a workflow on top of that, the hypothesis was checked rather than
assumed.

| Target | Result |
| --- | --- |
| 10 specs, dev server, 4 workers | 2 to 4 failures |
| 10 specs, dev server, 1 worker | 37 passed |
| **10 specs, production build, CI mode** | **74 passed, no flakes** |

One dev server transforming modules on the fly does not answer four browsers inside the assertion
timeout. A production build served by `vite preview` does, and the suite is stable there. So the
workflow builds first and runs against `preview` - that step is load-bearing, not an optimisation.

## The gate is a quarantine, and it says so

The full suite measured against that same production build: **42 passed, 42 failed** on chromium
alone. Running that as a required check would make CI permanently red, and a red gate gets ignored -
which is how this suite went unrun in the first place.

So the workflow runs the eleven specs known green, and the file states that deleting the filter is
the whole point: the moment the full suite passes, the list goes. Quarantining 42 known failures is
a worse state than 42 unknown ones, and this makes the number visible in one place.

## One more spec converted

`community-activities` joined the gate, using the round 69 recipe: `mockAppBootstrap` before its own
routes so the spec's mocks still win, plus `mockHelpCenter`. It seeds its own `communityId` and now
passes in isolation.

`community-decisions` was converted the same way and **reverted** - it still failed, so it has more
than a missing route. The file is back to its original state rather than half-patched.

## Verified

`gate-run-ci-mode.txt` is the gate exactly as the workflow runs it, with `CI=true`, against the
production build on port 3002: **76 passed** across both projects, no failures and no flakes.

`full-suite-preview.txt` is the unfiltered run that produced the 42/42, kept because the difference
between it and the gate run is the reason the gate is scoped.

No production code was touched, so the evidence is real test output.

## Known limitations

- The gate covers 11 of 47 spec files. The other 36 need the missing-route work done spec by spec,
  and `community-decisions`, `community-rooms-history` and `dashboard-guided-home` each need more than
  that.
- The two API-backed specs, `signal-detail-timeline` and `auth-edge-cases`, log in through the real
  form and need a seeded backend. That is the one blocker no amount of mocking removes.
- The local Vite dev server on 5299 died for the sixth time this session. The workflow avoids it
  entirely by testing a build, but local runs still depend on restarting it.
- Round 71's wart is still open: `vite.config.ts` says `server.port: 5173` while the `dev` script
  passes `--port 3002`, and `playwright.config.ts` defaults `baseURL` to 3002. The workflow sets the
  port explicitly so it is unaffected, but a developer following the config and the script will get
  two different answers.