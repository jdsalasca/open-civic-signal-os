# Round 86 evidence

Deliverable: a visual audit of the public backlog at mobile and desktop, with one suspected defect
investigated and found not to exist. No code changed, because there was nothing to change. Recording
that honestly is the result, not a failed round.

## Scope

Eleven rounds of this session changed test infrastructure and no production code. This round audited
the flagship public trust surface, `/backlog`, at `Pixel 5` (393px) and `Desktop Chrome` (1280px) and
checked it against the UX bar: hierarchy, spacing, contrast, consistency, empty state, responsive.

## Measurements

Route is `/backlog`, not `/public-backlog` - the first attempt 404'd. Screenshots were generated for
review and deliberately not committed, per the repository rule against committing generated `*.png`
artifacts from audits.

```
mobile   h1="Public backlog"  items=5  overflow=false  scrollW=393  winW=393  overflowing=[]
desktop  h1="Public backlog"  items=5  overflow=false  scrollW=1280 winW=1280 overflowing=[]
```

- No horizontal overflow at either width, and no element extends past the viewport.
- The page renders real content: 5 backlog items, the formula card, the freshness pill, and the
  footer call to action.
- Hierarchy reads correctly on mobile: eyebrow, `h1`, lede, freshness and count pills, the formula
  card in monospace, then the list.
- Empty state is designed rather than default: icon, "Nothing to show yet", an explanation that no
  problems have been reported publicly, and "Sign in to report a problem, vote, or follow a case."

## The suspected defect that was not one

The mobile screenshot appeared to render the formula label as `Formula versionv1` with no space. The
i18n template is correct - `"Formula version {{version}}"` in both EN and ES.

Rather than "fix" the template, the DOM was read directly:

```
text:       "Formula version v1"
codePoints: [...,110,32,118,49]   # 32 = space, between "version" and "v1"
html:       <div class="text-secondary mt-1 text-xs">Formula version v1</div>
```

The space is present. What looked merged was a narrow monospace space at screenshot resolution. No
change was made.

## Why this is the fifth thing that evaporated under checking

Rounds 76 to 82 turned up four apparent defects that were not defects: a "mock divergence" that was
never a mock, a testid that belonged to the view under test, a badge asserted in English that is
translated, and a `Pinned` text that exists as a boolean with no element carrying it. This is the
fifth. Each was plausible enough to act on, and each would have produced a diff that changed nothing
or made something worse.

The cheap, repeatable check that catches all of them is reading the DOM or the config value rather
than reading a screenshot or a comment. That has now paid for itself five times in a single session.

## Known limitations

- This audit covers one public route at two widths. Authenticated pages could not be audited: a fake
  token fails the auth guard, and no real account exists in the running backend, which is round 85's
  blocker.
- Screenshots were reviewed but are not in the repository, so this evidence is the measurements above
  rather than the images.
- Contrast was not measured numerically; it was assessed visually against both themes' tokens.
- The gate is unchanged at 20 specs, 94 passed.