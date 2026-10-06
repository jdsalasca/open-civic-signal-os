# Round 81 evidence

Deliverable: `community-open-data` is green and in the gate. Two specs that had been labelled
"needs a product decision" for several rounds were neither product decisions nor both the same
thing, and the plan said so without anyone checking.

## Both "product decisions" were stale test specs

`community-open-data` waited for a testid that does not exist:

```
community-open-data-token-scope-EXPORT_METRICS
```

`CommunityOpenData.tsx:446` builds its scope testids as:

```tsx
data-testid={`community-open-data-scope-${option.value}`}
```

with `option.value` being `EXPORT_${dataset.resource}` (line 86) - so the real testid is
`community-open-data-scope-EXPORT_METRICS`. The spec carried an extra `token-` segment. One segment
removed, spec passes: **1 passed (6.6s)**.

`community-official-announcements` waited for `pinned-announcements-section`, which had been recorded
as "belongs to another view". It is rendered by `CommunityBlog.tsx:471`, which is precisely the view
the spec navigates to. The spec was not looking in the wrong place.

Both were sitting behind a "product decision" label that nobody had tested. Recording that a thing is
undecidable is cheap, and it survives indefinitely because it looks like a conclusion.

## What announcements actually turned out to be

Not committed, because it could not be verified green this round. The diagnosis is here so the next
round starts from it.

1. `page.goto('http://127.0.0.1:5173/communities/blog')` - the hardcoded absolute URL to another
   project's port, the exact class round 73 fixed elsewhere. Relative path fixes that step.
2. With that fixed, the pinned assertion passes, and the failure moves to
   `await expect(page.getByText('Official')).toBeVisible()`. The badge is
   `t("community_blog.official_badge")`, so it is translated and an English literal cannot match in
   every locale - the same class as round 65. `official-timeline-section` is the stable testid.
3. With that replaced, the timeline renders a different post than the one the spec created:

```
"Chronological record ... Public Servant ... Road work update
 Repair crews resumed work today. 2026-03-04 - 08:00"
```

while the spec posted `official: true` with content "Official maintenance window this Saturday.". So
the POST mock is not feeding the list the view renders - the same shape as round 77, where the mock
state and the rendered board diverged.

That is the open question for round 82, and it is a mock-contract problem, not a product one.

## Verification

| Run | Result |
| --- | --- |
| `community-open-data` alone | 1 passed (6.6s) |
| full gate, CI mode, run list extracted from the workflow file | 92 passed, 0 failed, 0 flaky (2.3m) |

The gate reports `19 specs | duplicados: []`, printed from the file itself, so the command line
cannot drift from the workflow the way it did in round 77.

## Known limitations

- `community-official-announcements` still fails, one step further than before. Its spec file is
  deliberately unmodified in this commit rather than half-fixed.
- One gate run is not proof of determinism; round 80 ran twice for that reason and this change adds a
  spec rather than altering timing.
- 19 specs remain gated. The others are backend-dependent or blocked on the announcements mock.
- No production code changed, so this round carries no screenshots.