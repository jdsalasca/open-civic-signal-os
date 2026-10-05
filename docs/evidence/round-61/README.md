# Round 61 evidence

Deliverable: the workflow has now run on GitHub, and it found a real bug immediately.

## Three rounds of writing down that it had never run

Round 58 added the workflow and wrote: *"This workflow has not executed on GitHub's infrastructure. It is
verified to work on this machine and verified to avoid the two known Linux differences, which is not the
same as having seen it green in CI."* Rounds 59 and 60 each repeated the caveat.

The only way to close it is to actually trigger it, which means a real pull request. An empty one would
have proved nothing, so PR #122 carried the change that makes the gate worth having at all.

## What the PR changed, and why it was not an empty proof

The workflow fired on pull requests and on pushes to `main`. **Every change in this repository lands on
`develop` first**, so a gate scoped that way would have gone green by never running, on the branch where
the work happens. A check that reports success because it did not run is worse than no check: it looks
like coverage.

Also added `concurrency` with `cancel-in-progress`, because the job boots a container and applies 49
migrations and three pushes in a row should not leave three of them competing.

## What GitHub found that this machine could not

The first run **failed**, and not for a reason anyone here would have predicted:

```
[FATAL] Non-resolvable parent POM for org.opencivic:signalos-api:0.1.0:
spring-boot-starter-parent:pom:3.3.2 (absent): Cannot access central in offline mode
and the artifact ... has not been downloaded from it before.
```

`scripts/verify-postgres-schema.mjs` hardcoded `mvn -o`. Offline mode is fine on a developer machine,
because `~/.m2` is already warm after hundreds of local runs. A GitHub runner starts with an empty Maven
repository and cannot resolve even the Spring Boot parent POM in offline mode.

Fixed by making offline opt-in through `MAVEN_OFFLINE=1`, verified both ways locally, and pushed.

**This is the entire argument for running a gate instead of trusting it.** Three rounds of inspection
found nothing; the first execution found it in twenty seconds.

## What the first run also proved in the other direction

Before Maven, the log shows the Docker half succeeding on a real Linux runner:

```
Starting a throwaway PostgreSQL on 127.0.0.1:55432...
  49 migrations applied.
  Schema history is consistent.
```

Round 58 replaced `host.docker.internal` with a dedicated Docker network precisely because that name is
a Docker Desktop convenience that does not resolve on Linux. **That fix is now confirmed on Linux**, by
a runner that had never seen it before.

## Second run: green

```
schema-parity    pass    2m23s
hygiene          pass
adr-gate         pass
context          pass
evaluate         pass
ux-evidence      pass
```

And after the merge, the new trigger firing on the branch where the work happens:

```
event=push  branch=develop  ->  success  | Merge pull request #122
```

That is the whole point of the change: the gate now runs on `develop`.

## The PR hygiene gate failed first, and it was right to

My first PR body missed two of the repo's rules: no `#\d+` issue reference, and no `## Risks / Notes`
section. `scripts/agent-pr-hygiene.mjs` rejected it. Read the script rather than guessing, added both,
second run passed. Worth noting because the gate caught something I would have merged otherwise.

## Pre-existing failure found, not caused by this PR

`docker-images.yml` fails on develop with `Smoke test API container` unable to connect to port 18080.
Five consecutive runs before PR #122:

```
10/05 16:46 -> failure      10/05 14:25 -> failure
10/05 15:34 -> failure      10/05 10:57 -> failure
                             10/05 09:29 -> failure
```

Unrelated to this change - it builds and smoke-tests a container image. Merged anyway, and the reasoning
is written down rather than waved through: **the failing check is red on the base branch and is not
caused by this work**, so blocking on it would mean nothing can merge until it is fixed. It is the next
round's item.

## Verification

| Check | Result |
| --- | --- |
| `npm run schema:pg:verify` | 49 migrations, validate clean, four dimensions agree |
| Same with `MAVEN_OFFLINE=1` | green |
| PR #122 on GitHub, run 1 | **failed** - offline Maven, fixed |
| PR #122 on GitHub, run 2 | **passed** in 2m23s |
| Merge to develop, push trigger | **success** |
| Full suite, `clean` | 446 run, 5 skipped |
| `npm run agent:preflight` | passed |
| OpenAPI parity | 178 / 147 / 9 - unchanged |

## Known limits

- **The Docker smoke test is red on develop** and this round did not fix it. Next round.
- Cancellation is now reported as `cancelled` rather than `pass`, so a superseded run never reads as
  evidence. That is deliberate, but it means a red board can also mean "someone pushed again".
- Only one PostgreSQL major version is exercised, and only the one `infra/docker-compose.yml` pins.