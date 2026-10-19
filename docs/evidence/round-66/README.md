# Round 66 evidence

Deliverable: the case screen says what it is, and so does the topbar above it.

## The gap

Rounds 63 to 65 were found by looking at screens. This round is what looking turns up. The case screen
announced three things, and every one of them was a label belonging to something else:

```
eyebrow    "Priority Rank"          the title of the card below that renders the number 198
subtitle   "Intelligence Context"   the title of the card immediately underneath it
topbar     "What needs attention today / Home"
```

The topbar pair was the worst, and the cause was a single line in the shared `Layout`:

```tsx
const activeSection = [...nav].find((item) => item.to === location.pathname)?.label ?? t("nav.insights");
```

`item.to === location.pathname` is an exact match, and `/signal/2f2c...` is not a navigation
destination, so it never matched and fell through to `nav.insights` - whose value is literally
`"Home"`. Every case screen in the product was titled **Home**. The label above it,
`dashboard.focus_today` = "What needs attention today", is a dashboard framing applied to every route
that is not `/`.

None of it was wrong at the level of a string. All of it was wrong as an orientation: someone arriving
from a notification could not tell whether they were reading one case or a list.

## What changed

- `CivicPageHeader.description` is now optional, and the `<p>` renders only when there is one. A
  required subtitle is what forced this screen to invent a sentence; an empty slot is better than a
  wrong one, and every fact a subtitle could have carried is already in a labelled card or a badge.
- The eyebrow is now the category. "Priority Rank" stays where it belongs, heading the card that shows
  the number.
- `Layout` resolves the topbar in three steps: exact nav match, then longest-prefix match so a nested
  route stays under the section containing it, then an explicit map for destinations with no nav
  ancestor at all (`/signal/` -> `nav.case_detail`). Kept as data, so adding a detail screen is one
  line instead of another branch.
- The framing line renders only where it frames something. On a section route the dashboard copy is
  untouched, which is asserted rather than assumed.

## Red before green

`3 failed / 1 passed`, and the one green was **worthless**. `expect(page.getByTestId('page-header-description')).toHaveCount(0)`
passes when the testid does not exist yet, so it was green because the element was missing, not
because the screen was fixed. It now anchors on the title first, so it cannot pass without looking at
the screen.

After the fix: `14 passed` across the three signal-detail and backlog specs.

## The shared Layout is the risky part, so it got the wider run

`Layout` renders on every authenticated screen, so ten specs that exercise it were run together:
`15 passed, 1 skipped, 0 failed`. That covers navigation clarity, mobile drawer layering, theme
consistency, empty and restricted states, global state integrity and keyboard accessibility.

The dashboard assertion is the one that guards against over-correction: `/` must still say "What needs
attention today" and "Home", because that is what the dashboard is. A change that made every screen
quieter would have passed a narrower test.

## Reproducing

```powershell
npm --prefix apps/web-react run dev -- --port 5299 --strictPort
$env:BASE_URL="http://127.0.0.1:5299"
node scripts/capture-signal-detail.mjs docs/evidence/round-66
$env:BASE_URL="http://127.0.0.1:5299"
npx --prefix apps/web-react playwright test src/tests/signal-detail-describes-itself.spec.ts
```

## States captured

| Shot | What it shows |
| --- | --- |
| `in-progress-desktop.png` | Topbar "Case detail", eyebrow "INFRASTRUCTURE", no duplicated subtitle |
| `in-progress-desktop-timeline.png` / `-lower.png` | The audit trail and the seeded comment, unchanged by this round |
| `in-progress-mobile.png` | 390px, same corrections, no overflow |
| `resolved-desktop.png` | Green "Resolved" severity still intact |

## Known limitations

- Removing the subtitle leaves a taller gap between the title and the first card. It reads as
  whitespace rather than a break, and the right-hand column anchors that space, but it is a change in
  rhythm rather than a pure removal.
- `nav.insights` is still the fallback for an unrecognised route, and its value is "Home". It is a
  legitimate label for the Home nav item, which is why it was not renamed; a genuinely unknown route
  would still announce itself as Home. Renaming it means touching every nav label at once.
- Only `/signal/` is in the detail-route map. Other detail screens (a room, a proposal) still fall
  through to the prefix match or the fallback.
- The 26 pre-existing red specs recorded in round 65 are unchanged and still need a seeded backend.