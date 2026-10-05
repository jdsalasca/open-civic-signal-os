# Round 58 evidence

Deliverable: the PostgreSQL schema parity check runs on every pull request, and the script that
implements it works on Linux.

## What round 57 left as a manual step

Round 57 built `npm run schema:pg:verify` and wrote down the obvious gap: *"Not in CI yet - it needs
Docker, so it is a command a person runs rather than a gate that runs on every push."*

A check nobody runs is a check that decays. The migrations will keep moving; this one has to move with
them or it is decoration.

`.github/workflows/postgres-schema-parity.yml` runs it on pull requests and pushes to main, scoped by
`paths` to the migrations, the Java sources, the parity test, the script and `package.json` - so editing
a React component does not spend three minutes booting a database. `workflow_dispatch` for running it
by hand. Follows the repo's existing conventions: `permissions: contents: read`, `ubuntu-latest`,
temurin 21, surefire reports uploaded on failure.

## The script would have failed in CI, and that was the real work

Round 57's script reached PostgreSQL through `host.docker.internal`. That name is a Docker Desktop
convenience. **It does not resolve on a Linux runner**, so the script passed on a Windows machine and
would have failed on the first pull request - and the failure would have read as "the schemas disagree",
which is the one conclusion it must never draw wrongly.

Replaced with a dedicated Docker network and the container name:

```js
docker('network', 'create', NETWORK);
docker('run', '-d', '--rm', '--name', CONTAINER, '--network', NETWORK, '-p', `127.0.0.1:${PORT}:5432`, ...);
docker('run', '--rm', '--network', NETWORK, FLYWAY_IMAGE, 'migrate', `-url=jdbc:postgresql://${CONTAINER}:5432/${DB}`, ...);
```

Container name over a user-defined network resolves identically on Docker Desktop and on Linux. The
published port stays for the JVM side of the check.

Second portability fix: the parity test connects to `localhost`, which on a Linux runner can resolve to
`::1` first, while the published port is bound to the IPv4 loopback. Now `127.0.0.1`.

Both were found by reading the script against the runner it has to run on, not by running it. That is
the honest limitation here: **this workflow has not executed on GitHub's infrastructure.** It is verified
to work on this machine and verified to avoid the two known Linux differences, which is not the same as
having seen it green in CI.

## Also removed: a dead constant

`PostgresSchemaParityIT` had a hardcoded `URL` field the test never used - it reads
`POSTGRES_VERIFY_URL`. Removed rather than left as a second, stale answer to "where is PostgreSQL".

## Branch audit

The checkpoint asked for unmerged branches to be integrated. There were two, both from another tool:

| Branch | Content | Verdict |
| --- | --- | --- |
| `codex/critical-public-endpoints-anon-safety` | null-safe `resolveUsername` on anonymous endpoints | **already in develop** |
| `codex/critical-auth-degraded-coverage` | `AuthHardeningIT` degraded-resend test, Playwright spec | **already in develop** |

Neither was merged, because there was nothing left to merge. `SignalController` already resolves the
viewer username safely on both endpoints that permit anonymous access (`prioritized`, `top-10`), and
`GET /{id}` requires authentication so `authentication` is never null there. The degraded-resend test and
`auth-edge-cases.spec.ts` are both in HEAD.

Both left in place. They are another tool's refs, `git branch -d` would refuse them anyway, and deleting
unmerged branches on the strength of "it looks superseded" is how work disappears. Documented instead.

## Verification

| Check | Result |
| --- | --- |
| `npm run schema:pg:verify` after the network refactor | 49 applied, validate clean, schemas agree |
| Container and network cleanup | nothing left behind |
| Full suite, `clean` | **443** run, 2 skipped (parity pair without Docker) |
| `npm run agent:preflight` | passed |
| OpenAPI parity | 178 / 147 / 9 - unchanged |
| Workflow YAML | parses, basic structure present |

## Known limits

- **The workflow has never run on GitHub.** First PR to touch a migration is the real test. The two
  Linux differences above were addressed by inspection, not by execution.
- **`npm install --ignore-scripts`** is there because the script has no dependencies of its own but the
  npm script resolution expects the workspace. It is the minimum, not a considered dependency choice.
- Still **names, not types** (carried from round 57): a column whose type differs between engines while
  keeping its name would still pass.
- The three old stashes in the repository (`codex-pre-sync-*`, `temp runtime noise`) were left alone for
  the same reason as the branches.