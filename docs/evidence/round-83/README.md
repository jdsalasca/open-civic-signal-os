# Round 83 evidence

Deliverable: the mobile-chrome flake is fixed at its actual cause. Two unrelated specs were flaking
on the same browser project; neither had a locator problem, and the gate can now grow.

## What the symptom really was

Reproduced with retries disabled, on the project that had flaked:

```
Error: browserContext.newPage: Test timeout of 30000ms exceeded.
```

Not an assertion failure, not a detached node. Chromium could not create another browser context
within 30 seconds. The failure has no relationship to whatever the test was doing, which is why it
appeared against a different spec each round:

- Round 80: `community-trust-metrics` on mobile-chrome
- Round 82: `merge-review` on mobile-chrome

## The cause

```ts
workers: process.env.CI ? 1 : undefined,
```

Locally, `undefined` means Playwright defaults to half the cores and opens that many browser contexts
simultaneously. Emulated mobile projects are the expensive ones - Pixel 5 spins up a full emulation
context each - so the context pool is what runs out, not the assertions.

CI sets `CI: 'true'`, so it already ran at one worker. The gate and CI disagreed, and the flake only
ever existed on the side of that disagreement where verification happened.

## The fix

```ts
workers: 1,
```

One worker everywhere. Local verification is now identical to CI rather than being a second, weaker
posture that can disagree with it.

## Verification

| Run | Result |
| --- | --- |
| the two flaky specs, mobile-chrome, `--repeat-each=4 --retries=0` | 32 passed (1.7m) |
| full 19-spec gate, both projects, `--retries=0` | 92 passed, 0 failed, 0 flaky (4.5m) |

`--retries=0` is the point. Every previous gate run allowed retries locally only when `CI` was set,
so a retry silently absorbed the flake and the run still reported green. With retries off, a green
result means the run genuinely did not need rescuing.

## What rounds 79 and 80 actually cost

Rounds 79, 80 and 82 each reverted the gate, once over a flake I had attributed to a specific spec.
Two of those fixes - scoping `community-trust-metrics`'s dropdown locator in round 80, and the
`merge-review` locator work implied in round 82 - were addressing a symptom. The round 80 locator fix
is still correct on its own terms, since an unscoped `getByText` on a dropdown option is genuinely
ambiguous, but it was not the flake.

Chasing flakes one at a time would have kept reverting the gate indefinitely, because each round
fixed whatever surfaced next and never asked why the surface kept moving.

## Known limitations

- One full gate run is not proof of determinism, but the mechanism is now understood rather than
  inferred, and the reproduction that failed on demand now passes 32 times with retries off.
- `community-official-announcements` is still verified only in isolation; it joins the gate next
  round now that the blocker is gone.
- Serial execution makes local runs slower: 4.5m for the gate against 2.3m before. That is the cost of
  local and CI meaning the same thing.
- The remaining ungated specs are backend-dependent. No production code changed, so no screenshots.