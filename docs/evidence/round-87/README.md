# Round 87 evidence

Deliverable: a re-verified baseline. The gate was run end to end after the automated documentation
commits landed on `develop`, to confirm the tree this session produced is still green.

```
gate: 20 specs | duplicados: []
94 passed, 0 failed, 0 flaky   (--retries=0, both projects, 5.7m)
```

## Why this round is a verification and not a feature

The two remaining plan items are the same blocker seen from two sides:

1. No SUPER_ADMIN is seeded, so four specs fail at login. Deciding the profile, the environment and
   the password policy is a backend decision with security weight, not a call to make inside a test
   round.
2. The Playwright workflow starts no backend at all, so those four specs could not join the gate even
   if an admin existed. They need a second CI job with the full stack up.

Neither can be closed and verified inside this round's budget, and starting one without the
verification step would produce an unproven diff and a dirty tree. So this round closed the gate
question that was actually open - is the repository still green after everything - and recorded it.

## What the number means

`--retries=0` with `workers: 1`, both browser projects. This is the same posture that exposed the
`browserContext.newPage` timeouts in rounds 79 to 82, which is the point: a green result here means the
suite did not need rescuing. Round 84 reported 94 passed under this posture too, so two independent
runs at the stricter setting agree.

## Session state at this point

| Item | State |
| --- | --- |
| Playwright gate | 20 specs, 94 passed, 0 flaky, retries disabled |
| Production code changed this session | none |
| Specs fixed | 7 (`community-projects`, `community-proposals`, `community-open-data`, `community-official-announcements`, plus the gate's own additions) |
| Root causes found | 4 distinct classes, all in test infrastructure, none in the product |
| Unverified claims retired | 5 |
| Worktrees / branches left behind | none |

## Known limitations

- Four specs remain ungated for the two reasons above and none of them has been run to completion.
- Authenticated pages still cannot be audited or tested without a seeded account.
- This round produced no code. The honest summary of this session is that it made the safety net
  honest, and proved it by running it.