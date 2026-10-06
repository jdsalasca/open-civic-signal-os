# Round 89 evidence

Deliverable: the backend-spec blocker proven end to end, after three rounds of asserting it without
proof. No code changed; the fix is operational and belongs to a stack decision.

## The proven cause

The running container was created from the **prod** compose file, not the dev one:

```
docker inspect civic-api --format '{{...config_files}} | {{...service}}'
  ...\infra\docker-compose.yml | civic-api

docker exec civic-api env | grep SPRING_PROFILES_ACTIVE
  SPRING_PROFILES_ACTIVE=prod
```

`seedUsers` is annotated `@Profile({"dev", "test"})`, so under `prod` it is correctly excluded and the
database has no seeded users. Login confirms it, and the message is specific rather than a rate limit:

```
POST /api/auth/login  {"username":"admin","password":"admin12345"}
  401  {"message": "Invalid credentials.", "error": "Unauthorized"}
```

`AuthController.login` is `@PostMapping("/login")` under `@RequestMapping("/api/auth")` and takes
`LoginRequest(String username, String password)`, so the request shape the specs use is correct. The
credentials match `docs/CHANGELOG.md:58` and the seeder. Only the profile is wrong.

## What rounds 85, 87 and 88 got wrong

- **85** said "no seeded admin" - the conclusion was right, but it rested on a probe of 8080 that
  belongs to another project's container, so the reasoning was unsound.
- **87** said seeding an admin was "a backend decision with security weight" - wrong. The seeder
  exists and is deliberately profile-gated; no new decision is needed.
- **88** retracted 87 and then partially retracted 85. It was right that the seeder exists and wrong
  to imply the account was simply absent without checking the profile.

Reading the code, then the container's actual environment, is what settled it. Three rounds of
inspection by eye did not.

## The resolution, and why it is not done here

`infra/docker-compose.dev.yml` already sets `SPRING_PROFILES_ACTIVE: dev`, so the documented
`npm run docker:dev:up` would seed `admin`, `servant` and `citizen` with three communities and
memberships. It cannot run on this machine: `civic-mail` from the prod stack already holds 8025, so
`infra-civic-mail-dev-1` fails to bind.

That is a genuine developer-experience trap and a real defect in the workflow, but resolving it means
choosing between stopping a running stack and parameterising the mail port - a stack decision, not a
test round. The four orphaned containers from the failed attempt were removed; the running `civic-*`
stack was left alone.

## After this, the four specs should run

`community-buttons`, `community-threads-paging`, `signal-detail-timeline` and `auth-edge-cases` all
log in as `admin` with the seeded credentials. With the dev profile active they have a real chance, and
they still cannot join the gate, because the workflow starts no backend at all - they need a second CI
job.

## Known limitations

- No code changed and no spec was run end to end. The account's existence under the dev profile is
  inferred from the seeder source and the annotation, not observed, because the dev stack cannot bind
  here.
- The port collision between the prod stack and the dev stack is unresolved.
- The gate is unchanged at 20 specs, 94 passed, retries disabled.