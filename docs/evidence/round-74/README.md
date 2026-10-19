# Round 74 evidence

Deliverable: one spec brought to green, two genuinely broken locators fixed, and the remaining
failures narrowed to a question about view state rather than about the test harness.

## Two defects fixed

**`community-trust-metrics` now passes.** Round 73 gave it the relative-URL fix but not the bootstrap,
so it was still missing `auth/me` and `help-center`: those reached the live backend, 401'd, the
refresh returned 403, and axios logged out. The test then asserted on a login page.
`mockAppBootstrap` plus `mockHelpCenter` and it passes in 5 seconds.

**Two ambiguous locators in `community-proposals`.** Both were strict-mode violations, which is the
fragile-selector failure `AGENTS.md` warns about:

```
strict mode violation: getByText('Unsafe pedestrian crossing', { exact: true })
  resolved to 2 elements
strict mode violation: getByText('Evidence', { exact: true })
  resolved to 4 elements
```

The first title renders both as the proposal title and as a related-signal link; the second is a
deliberation-type option that also appears as a label. Both clicks are dropdown selections, and the
repo already had the right pattern in `community-threads-paging.spec.ts`:

```ts
await page.locator(".p-dropdown-item", { hasText: "Unsafe pedestrian crossing" }).click();
await page.locator(".p-dropdown-item", { hasText: /^Evidence$/ }).click();
```

After both, the test advances past the clicks and now waits for
`proposal-deliberation-moderate-8888...`, which is a render-condition question rather than a harness
one.

## What the remaining six need, and why the recipe stopped working

| Spec | Reaches | Blocker |
| --- | --- | --- |
| community-proposals | past the dropdowns | `proposal-deliberation-moderate-<id>` never renders |
| community-projects | creates a board, then expects its name | the mocked POST response is not what the view stores |
| community-governance | creates a document, expects its title | same shape of question |
| community-open-data | waits for `community-open-data-token-scope-EXPORT_METRICS` | that testid exists in no view; the card offers `community-open-data-scope-list` |
| community-official-announcements | waits for `pinned-announcements-section` | exists in `CommunityBlog`, not in the view this spec drives |
| community-decisions | bootstrap applied this round | still failing further in |

Two of these are worth calling out because they are not the same problem as the rest:

- `community-open-data-token-scope-EXPORT_METRICS` **exists in no view at all**. Either the view never
  grew per-scope hooks or the spec was written against a design that did not ship. That is a question
  about intent, so it is recorded rather than guessed.
- `pinned-announcements-section` exists, but in `CommunityBlog` - a different view from the one this
  spec drives. The test and the view disagree about which screen owns pinned announcements.

## Gate

`community-trust-metrics` joined the gate: **14 of 47 spec files, 82 passed** in `CI=true` mode
against the production build, no failures and no flaky. Up from 80 at round 73, and the two flaky from
round 73 did not recur.

The round-73 flakiness was not repeated here, but a single clean run is not proof it is gone; both
`community-integrations` and `community-resources` remain load-sensitive.

## A note on how this round's failures were found

Every step was verified against the saved output rather than the console tail, because round 69
produced a run whose last line read `35 passed` while the file said `2 failed`. That habit is the only
reason these numbers are trustworthy.

## Known limitations

- Six specs still fail, on view state rather than on routing or selectors.
- Two of the six need a decision about product intent before a test can be written: whether the
  open-data token scopes should have per-scope test hooks, and which screen owns pinned announcements.
- No production code was touched, so the evidence is test output rather than screenshots.
- Local runs use port 3123 because 3002 is contested by another project on this machine; the CI
  workflow is unaffected since it runs on an isolated runner.