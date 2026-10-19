# Round 68 evidence

Deliverable: two more specs that had never run now run, and the helper that makes the rest a
one-liner is finished.

## What this round did

Round 67 established the pattern: no workflow runs this suite, so "26 failing" never meant anything,
and the dominant cause is specs missing routes the app fires. Round 68 applied that to
`dashboard-aging`.

## The cause here was the same one, and the diagnosis was cheap this time

`dashboard-aging` mocked `signals/meta`, `signals/prioritized`, `signals/duplicates`, `signals/aging`,
`notifications/recent` and `communities/my`. It did not mock `auth/me` or `help-center`. Both reached
the live backend, 401'd, the refresh returned 403, and axios called `logout()` - so
`dashboard-aging-panel` never rendered.

Unlike round 67, no loop had to be chased: reading which routes the view calls answered it.

## The ordering trap, and why the helper is shaped the way it is

`dashboard-aging` asserts on real aging content, so it keeps its own aging payload. Calling
`mockDashboardRoutes` after that would have silently replaced it, because **Playwright checks the most
recently added route first**. So:

- `mockHelpCenter(page)` is exported separately for specs that bring their own aging data.
- `mockDashboardRoutes(page)` is aging plus help-center, for specs that have no opinion on either.
- The bootstrap is registered *before* the spec's own routes in each spec, so the spec's routes win.

Getting this wrong would have produced a green test asserting on the helper's empty payload instead of
the spec's data, which is worse than a red one.

## A second trap: two seeds competing

Each test in `dashboard-aging` seeded its own session inline with `addInitScript`. Adding
`seedAuthenticatedApp` on top meant two seeds writing the same key, and the last one won - so the
"hides the aging panel from citizens" test was running as staff and asserting the opposite of what it
was named. The inline blocks were removed and the role is now a parameter:
`seedAuthenticatedApp(page, "CITIZEN")`.

## Red before green

```
dashboard-aging  1 failed / 1 passed   (citizen test running with a staff session)
after            2 passed (3.2s)
```

Run alongside everything this session has touched: **20 passed**, in `playwright-run.txt` verbatim.

## What was attempted and deliberately reverted

`community-rooms-history` was converted the same way. The session then survived - the logout was
genuinely fixed - but `community-rooms-history-note` still did not render, because the rooms view needs
an **active community** and `communities/my` returns an empty list. This is the same trap as the blog
capture abandoned in round 65: the store clears the active community when memberships come back empty.

The file was reverted rather than left half-patched. It needs a seeded membership, which is its own
round, not a two-line edit bolted onto this one.

## Also found, not fixed

`package.json` has `"dev": "vite --port 3002"`. Running `npm run dev -- --port 5299` therefore produces
`vite --port 3002 --port 5299`: it works because Vite takes the last flag, but every documented port
override emits a duplicated flag, and the default belongs in `vite.config.ts` as `server.port` so the
script can be plain `vite`. It is a two-line change and a separate objective, so it is queued rather
than smuggled in here.

## Known limitations

- Still unconverted: `weekly-digest`, `merge-review`, `field-data-mode`, `community-rooms-history` and
  `dashboard-guided-home`. The last one needs seeded memberships and a role switch; the fourth needs
  the same. The first three have their own copy of `seedSession` that the shared helper now replaces,
  but each still needs its own missing-route diagnosis.
- `field-data-mode` calls its local `seedSession(page, dataMode)` with a second argument, so it needs
  the role parameter to cover data mode too rather than a plain swap.
- The local Vite dev server on 5299 died for the fourth time this session. It is launched detached and
  something reaps it; unrelated to these defects, but it makes long Playwright runs flaky.
- No production code was touched, so the evidence is real test output rather than a screenshot.