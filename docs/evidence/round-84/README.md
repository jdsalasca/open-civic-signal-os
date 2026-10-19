# Round 84 evidence

Deliverable: `community-official-announcements` joins the gate. The gate is 20 specs and reports
**94 passed, 0 failed, 0 flaky** with `--retries=0`.

## What this round is really about

Three consecutive rounds reverted this gate change for the same reason: a mobile-chrome flake. Round
83 found the cause (`workers: CI ? 1 : undefined`, so local runs opened a context per core and ran
out of them) and fixed it. This round is the payoff - the change that had been blocked three times now
lands, verified under the stricter posture that exposed the original problem in the first place.

## The blocker, for the record

- Round 79: 89 passed, 1 flaky, `community-trust-metrics` on mobile-chrome. Reverted.
- Round 80: 90 passed twice, but that was with retries on, which can absorb a flake and still report
  green. Locator fix committed, gate reverted anyway for honesty.
- Round 82: 93 passed, 1 flaky, `merge-review` on mobile-chrome. Reverted again.
- Round 83: root cause found and fixed. 92 passed with `--retries=0`.
- Round 84: 94 passed with `--retries=0`, 20 specs.

The difference between round 82 and round 84 is not luck. Round 82 had retries enabled on the
mobile project and the flake arrived in a different spec than round 79's; round 84 disables retries,
runs one worker, and holds.

## Why `--retries=0` is the meaningful number

Every gate run before this one would have reported green even while a flake was being retried away.
The config sets `retries: process.env.CI ? 2 : 0`, so local runs with `CI` set silently gained two
retries per test. That made a flaky suite indistinguishable from a deterministic one, which is
precisely why three rounds of chasing were needed before the cause was found.

A green run with retries disabled means the suite did not need rescuing. That is the first such claim
in the sequence.

## Adding the spec

One inserted line, and the run list is read back out of the workflow file rather than hand-written,
which prints `gate: 20 specs | duplicados: []` before the run. Rounds 77 and 79 both drifted between
the command line and the file; this is the mechanical guard against that, and it is why the count can
be trusted here.

## Known limitations

- One green run with retries disabled is strong evidence, not proof. The next gate change should be
  made after a second run, and the habit of running twice is worth keeping.
- Local gate runs are serial now, so about 3m for 20 specs across two projects. Slower than the 2.3m
  parallel version, but that version was not measuring anything real.
- The remaining ungated specs need a seeded backend, not locator work.
- No production code changed, so no screenshots.