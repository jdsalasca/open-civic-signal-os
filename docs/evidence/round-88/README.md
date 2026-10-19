# Round 88 evidence

Deliverable: the round 85 blocker located precisely, plus a correction of my own over-correction. No
code changed, because the two things I believed this round both turned out to be wrong.

## The seeder already exists

`OpenCivicSignalOsApplication.seedUsers` is a `CommandLineRunner` gated by `@Profile({"dev","test"})`.
It seeds `admin`/`admin12345` with `ROLE_SUPER_ADMIN`, plus `servant` and `citizen`, three
communities, and memberships for all of them.

So "seeding a SUPER_ADMIN is a backend decision with security weight" - what round 85 and round 87
both said - was wrong. The decision is already made and written down: the profile. What was missing
was activation.

## Correction one: the civic stack was already running

Round 85 concluded the blocker was "no seeded admin" partly because "a backend is already running" -
`localhost:8080` and `localhost:8081` answering `/actuator/health`. That probe was sloppy and I have
to correct it:

```
civic-api   Up 9 hours (healthy)   0.0.0.0:8081->8080/tcp
civic-db    Up 19 hours (healthy)  0.0.0.0:5432->5432/tcp
civic-mail  Up 9 hours (healthy)   0.0.0.0:8025->8025/tcp
```

`8081` is this project's API. The `8080` that answered health belongs to `open-university-os-backend`,
another project on this machine.

## Correction two: I then over-corrected, and caught it

Mid-round I concluded "there is no civic backend running at all" from `docker ps`. That was wrong
too: `civic-api` is up and healthy on 8081. Two wrong beliefs in one round, in opposite directions,
both from shallow inspection.

## What is actually unresolved

The login POST to 8081 returned an HTTP error status, not a connection failure, so the API answered.
My request shape is the unproven part - I used `/api/auth/login` with `username`/`password` without
first reading the auth controller's actual mapping or DTO. So the honest statement is:

- The seeded `admin` user should exist, because the dev profile seeds it and the container is up.
- Whether it exists in *this* container's database was not confirmed.
- The login attempt's failure has not been attributed, and this round does not claim a cause.

Round 85's write-up said the blocker was a missing admin account. That remains unproven and should be
treated as unproven.

## Environment note

`npm run docker:dev:up` fails on this machine: `civic-mail` from an earlier session already holds
8025, so `infra-civic-mail-dev-1` cannot bind. The four `infra-civic-*-dev-1` containers that the
failed attempt left in `Created` state were removed, so no orphan worktrees, branches or containers
remain. The running `civic-*` stack was left untouched - it is serving 8081 and was not mine to stop.

## Known limitations

- No code changed. The next round needs to read the auth controller's real mapping before making any
  claim about login, and then run one of the four backend specs end to end.
- The port collision between the compose dev stack and the session's own running containers is
  unresolved and will block any future `docker:dev:up` on this machine.
- The gate is unchanged at 20 specs, 94 passed, retries disabled.