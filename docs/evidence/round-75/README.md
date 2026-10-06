# Round 75 evidence

Deliverable: two more specs brought to green by fixing a defect class rather than a single instance,
and the gate grown to 16 of 47 spec files with 86 passing tests.

## The defect was a class, not an incident

Round 74 fixed two strict-mode violations in `community-proposals`. Opening `community-projects`
this round produced a third:

```
strict mode violation: getByText('Safer school crossing', { exact: true })
  resolved to 2 elements
```

Same shape every time: a bare `getByText` used to pick a dropdown option, where the same string also
renders somewhere else - as the proposal title, as a related-signal link, as a form field label. It
is the fragile-selector failure `AGENTS.md` warns about, and it had already produced three failures
across two specs before anyone counted them.

A grep for the shape found **13 occurrences of `getByText(...).click()`** across the suite. That made
it a sweep rather than a hunt.

## Fixed here: five occurrences, two specs green

`community-decisions` (three) and `community-governance` (one) now scope the click to the dropdown
item, using the pattern the repo already had in `community-threads-paging.spec.ts`:

```ts
await page.locator(".p-dropdown-item", { hasText: "..." }).click();
```

Both specs went from failing to **passing in 8.7 seconds combined**. `community-projects` got the same
fix and advanced past the ambiguous click, but now waits for `project-board-submit-button` to become
stable, which is a different problem and is not resolved here.

Only specs being actively converted were touched. The remaining bare `getByText` clicks live in
specs with no evidence of ambiguity today - `help-center`, `role-switch-ux`,
`profile-identity-settings`, `dashboard-guided-home`, and one in `community-trust-metrics` which
passes. Converting them on suspicion would risk breaking specs that are green for a speculative
reason.

## The static-mock question, answered

`community-projects` was worth understanding before guessing at it. After creating a board the view
calls `await loadBoards()` and then selects the new one, so the created board only appears if the list
refetch returns it. The spec's mock already handles that correctly: `boards` is declared with `let`
and the POST handler does `boards = [newBoard, ...boards]`. The mock was realistic and the failure was
somewhere else entirely - which is why the fix went to the locator and not to the fixture.

That is worth stating because the natural assumption, that a create-then-assert spec fails because
its mock is static, was wrong here.

## Gate

Two specs added: **16 of 47 files, 86 passed** in `CI=true` mode against the production build, no
failures and no flaky. Up from 82 at round 74.

## Still failing, honestly

| Spec | Reaches | Blocker |
| --- | --- | --- |
| community-projects | past the dropdown | `project-board-submit-button` never stabilises |
| community-proposals | past both dropdowns | `proposal-deliberation-moderate-<id>` never renders |
| community-open-data | waits for `community-open-data-token-scope-EXPORT_METRICS` | that testid exists in no view |
| community-official-announcements | waits for `pinned-announcements-section` | exists, but in a different view |

The last two are questions about product intent - whether token scopes should have per-scope test
hooks, and which screen owns pinned announcements - and a test cannot be written until someone
decides.

## Known limitations

- Four specs still fail, and two of them need a decision rather than a fix.
- Nine bare `getByText(...).click()` locators remain in specs that currently pass. They are
  recorded as a worklist, not changed, because changing a green spec on suspicion is how a suite
  starts lying.
- No production code was touched, so the evidence is test output rather than screenshots.
- Local runs use port 3123 because 3002 is contested by another project on this machine.