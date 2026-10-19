# Round 94 evidence

Deliverable: the blocker chased since round 85 is closed and proven. The seeded `admin` account logs
in successfully against the dev API. Four rounds of assertion became a fact.

## The login works

```
POST http://localhost:18081/api/auth/login
  {"username":"admin","password":"admin12345"}

  200  { accessToken, refreshToken, role, username }   accessToken length 174
```

`admin-login.txt` is the raw output. The account exists, is verified and is enabled, because
`seedUsers` ran under the `dev` profile this time. Round 89's diagnosis is confirmed end to end:
wrong profile, no seeder, no account, `401 Invalid credentials.`

One detail worth keeping: the response carries `role`, not `activeRole`. Earlier probes in rounds 85
and 88 guessed field names instead of reading `AuthResponse`, which is part of why they were
inconclusive.

## Round 93's window change was the thing that made this possible

```
docker inspect infra-civic-api-dev-1 -> HEALTHY tras 6.8 min
```

The cold build took just under seven minutes. Round 91's `start_period: 180s` would have marked this
container unhealthy at three minutes, exactly as round 93 observed, and `docker:dev:up` would have
failed or lied in the other direction. Round 93's `start_period: 600s` / `retries: 120` gave it enough
room to finish, which is now measured rather than assumed.

## What this unblocks

`community-buttons`, `community-threads-paging`, `signal-detail-timeline` and `auth-edge-cases` all log
in as `admin` with these credentials. Nothing about them was wrong - they were blocked on a runtime
that did not exist. They still cannot join the Playwright gate, because that workflow starts no
backend; they need a second CI job with the full stack, which is now buildable rather than hypothetical.

The frontend specs also become reachable for the first time in this session: `vite.config.ts` proxies
`/api` to `VITE_PROXY_TARGET` (default `http://localhost:8081`), so a preview pointed at
`http://localhost:18081` can reach this API. That unlocks authenticated pages, which have been
impossible to audit or test since round 85.

## Environment left running

Both stacks are up: the pre-existing prod stack (`civic-api` on 8081) untouched, and the dev stack on
overridden ports 18081 / 15173 / 18025 / 15432.

## Known limitations

- No spec has been run against this API yet; the login is the first successful authenticated call.
- The gate is unchanged at 20 specs, 94 passed, retries disabled.
- The second CI job for backend specs is still unbuilt.