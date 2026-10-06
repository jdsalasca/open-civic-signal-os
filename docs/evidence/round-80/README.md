# Round 80 evidence

Deliverable: the `community-trust-metrics` mobile-chrome flake is fixed, and `community-projects`
is in the gate for real this time. Two consecutive CI-mode runs of the 18-spec gate are identical:
**90 passed, 0 failed, 0 flaky**.

## The flake

Round 79 widened the gate and hit it:

```
89 passed, 1 flaky
  1) [mobile-chrome] > src/tests/community-trust-metrics.spec.ts
     "shows explainable trust cards and degrades cleanly for low-data communities"
```

Six consecutive runs on mobile-chrome in isolation passed (`trust-metrics-repeat6.txt`), so the
failure needed the parallel-run context rather than the browser.

## The wrong hypothesis first

The freshness assertion reads `toContainText("20m ago")`, which looks like a clock boundary: if the
mock computed the timestamp from `Date.now()`, a run crossing a minute under load would see `21m ago`
and fail. It does not. The mock returns the literal string `freshness: "20m ago"`, so that assertion
cannot drift.

## What it actually was

```ts
await page.getByTestId("community-trust-period-filter").click();
await page.getByText("Last 7 days", { exact: true }).click();
```

An unscoped `getByText` on a PrimeReact dropdown option. It can match the Select's own label or a
stale overlay node, either of which can resolve to a detached element once other workers put the
browser under load - which is exactly why it passed in isolation and flaked in the full gate.

This is the third spec with this defect and the third fix of the same shape. `community-decisions`
and `community-governance` were scoped to `.p-dropdown-item` in round 75; this one is now:

```ts
await page.locator(".p-dropdown-item", { hasText: "Last 7 days" }).click();
```

No production code changed.

## The gate, verified by reading the file back

Round 77 claimed a gate change that never landed, because a string replace renamed an entry instead
of adding one and the verification passed extra files on the command line. Both failure modes are
avoided here:

- The edit is a single inserted line, not a replace.
- The run list is extracted from the workflow file itself, not hand-written, so the command line
  cannot drift from the file. The script prints the count and any duplicates before running:
  `gate: 18 specs | duplicados: []`.

`community-projects` had never actually been in the gate despite round 77's commit message. It is
now.

## Verification

| Run | Result |
| --- | --- |
| spec alone, mobile-chrome, `--repeat-each=6` | 6 passed (32.8s) |
| full gate, CI mode, run 1 | 90 passed, 0 failed, 0 flaky (2.3m) |
| full gate, CI mode, run 2 | 90 passed, 0 failed, 0 flaky (3.3m) |

Two identical runs is the minimum for a claim about a flake. One green run would have been
consistent with luck; the round 79 run proved a single green run proves nothing.

## Known limitations

- Two runs is better than one and still not proof of determinism. The locator fix removes a known
  ambiguous-node class regardless of whether this was the only contributor.
- `community-open-data` and `community-official-announcements` still need a product decision: whether
  token scopes should have per-scope test hooks, and which screen owns pinned announcements.
- 18 of the suite's specs are gated; the rest are either backend-dependent or awaiting those
  decisions. No production code changed, so this round carries no screenshots.