# Round 79 evidence

Deliverable: `community-proposals` is green. The spec's moderation flow now runs end to end.

## The cause was the round 76 regression, unfixed in this one spec

The test waited for the moderation control and never saw it:

```
locator.click: Test timeout of 30000ms exceeded
  55 × waiting for element to be visible, enabled and stable
```

`CommunityProposals.tsx:147`:

```ts
const canModerate = activeMembership?.role === "COORDINATOR" || activeMembership?.role === "PUBLIC_SERVANT_LIAISON";
```

The spec seeded `role: "COORDINATOR"` in two places and already declared
`membershipRole: "COORDINATOR"` on one helper call - then called `mockAppBootstrap(page, communityId)`
without it, so the default `MEMBER` overwrote the membership and the control was never rendered.

This is exactly the downgrade found in round 76. The fix added there was never applied to this spec,
which is the second spec to need it and the proof that the fix has to be applied at every call site,
not once in the helper. The one-line change passes the role through.

## The gate change is deliberately not in this commit

Round 77 claimed `community-projects` had joined the gate. It had not. The edit used for that was a
string replace that renamed the existing `community-proposals` entry instead of adding a new one, so
the file held 17 entries with `community-proposals` present and `community-projects` absent. The
round 77 verification passed 18 files on the command line, which is why the mistake was invisible:
the command line and the workflow file had drifted apart.

Adding `community-projects` correctly makes the gate 18 specs, and that run exposed a blocker:

```
89 passed, 1 flaky
  1) [mobile-chrome] > src/tests/community-trust-metrics.spec.ts
     "shows explainable trust cards and degrades cleanly for low-data communities"
```

It passed on retry, so it is a genuine flake, not a failure. A gate that needs a retry is not
deterministic, and round 75's baseline was explicitly zero-flaky. Committing an 18-spec gate with an
uninvestigated flake in it would trade a real regression for a cosmetic one, so the gate change is
reverted and the flake is the next round's work.

Verification therefore rests on the spec alone: `proposals-after-role.txt`, 1 passed (9.5s). That is
the honest scope of what this commit changes.

## Known limitations

- `community-trust-metrics` is flaky on mobile-chrome and is now in the gate, so this needs fixing
  before the gate can grow again.
- The gate file lists 17 specs while the suite has more; any future gate edit must be verified by
  reading the list back out of the file, not by assuming a replace did what was intended.
- `community-open-data` and `community-official-announcements` still need a product decision.
- No production code changed, so this is test output rather than screenshots.