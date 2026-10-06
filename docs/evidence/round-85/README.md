# Round 85 evidence

Deliverable: a blocker characterised precisely enough to act on. This round did not produce code
value, and the honest reason is recorded below rather than dressed up.

## The "needs a seeded backend" claim, finally checked

Four specs have been recorded as blocked for several rounds because they log in as a real admin and
create data through `page.request`: `community-threads-paging`, `community-buttons`,
`signal-detail-timeline`, `auth-edge-cases`. Unlike the previous seven blockers, this one had never
been tested, so it stayed an assumption.

Two things turned out to be different from the assumption:

1. **A backend is already running.** `localhost:8080` and `localhost:8081` both answer
   `/actuator/health` with 200, and `vite.config.ts` proxies `/api` to `http://localhost:8081` by
   default. No Docker startup was needed.
2. **So the blocker is narrower than "needs a backend".** It is specifically the admin account.

## The actual failure

```
Error: page.waitForURL: Test timeout of 30000ms exceeded.
  waiting for navigation to "**/" until "load"
```

The login form fills, the submit is clicked, and no navigation to the app ever happens - the
credentials are rejected, or the account is absent. `docs/CHANGELOG.md:58` documents the account the
spec is using:

> **SuperAdmin Role**: Introduced `ROLE_SUPER_ADMIN` (`admin` / `admin12345`) with full system
> access.

So the spec's constants are correct per the changelog. The running instance simply has no seeded
admin, which points at the seed path or the profile it runs under rather than at the tests.

`community-buttons-real-backend.txt` is the raw run.

## Why nothing was changed here

The obvious workarounds were all worse than the finding:

- Skipping the spec when login fails would hide the gap behind a green suite, which is the failure
  mode this whole session has been removing.
- Creating the admin through the public API would need elevation the API deliberately does not
  expose.
- Editing the spec's credentials would break it against the changelog and against any environment that
  does seed correctly.

Seeding a SUPER_ADMIN is a backend decision with security weight - which profile, which environment,
what the password policy is. That is not a call to make inside a test round, so it is written down
with the exact failure, the exact credentials, and the changelog line that justifies them.

## Known limitations

- Three of the four blocked specs remain unrun. Their blocker is now "no seeded admin", not "no
  backend", and all three fail the same way at the login step.
- The Playwright workflow starts no backend at all, so even with an admin seeded these specs cannot
  join that gate. They would need a second CI job with the full stack up.
- The gate remains 20 specs, 94 passed, unchanged by this round.