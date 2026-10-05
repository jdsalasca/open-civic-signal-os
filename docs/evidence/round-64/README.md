# Round 64 evidence

Deliverable: the signal detail screen speaks plain language, and the shared primitives that make
that possible now live in one place instead of two copies.

## The gap

Round 63 fixed the public backlog. The same three leaks were still present one file over, in the
screen where a resident follows a single case over time - the audit trail itself:

```
IN_PROGRESS   Status changed   1.4.2026, 10:00:00   From NEW to IN_PROGRESS
```

Four sites in `SignalDetail.tsx` published machine output: the header badge (raw `signal.status`),
the timeline badge (raw `entry.statusTo`), the transition sentence (raw `statusFrom`/`statusTo`), and
the entry timestamp (`toLocaleString()`, which is the identical locale defect round 63 fixed).

`SignalDetail` was not a special case. A grep found **24 `toLocaleString` /
`toLocaleDateString` / `toLocaleTimeString` call sites across 15 files**, all with the same two
problems: locale-dependent shape, and seconds on a source that is a `LocalDateTime`.

## What changed

Two shared modules, then applied:

- `src/utils/formatStamp.ts` - fixed `YYYY-MM-DD HH:mm` built from local date components, so no
  offset is invented for an offset-less `LocalDateTime`, and no locale lookup decides the shape.
  Returns `undefined` for missing or unparseable input so callers keep their own fallback.
- `src/utils/signalStatus.ts` - `useSignalStatusLabel()`, the single lifecycle map. An unmapped
  value resolves to "Unknown" rather than falling through to the raw string, so a status added on
  the API cannot leak an identifier into the UI on the day it ships.

`PublicBacklog` no longer holds its own copies: it imports both. The i18n keys moved from
`public_backlog.*` to `signals.*` rather than existing twice.

Before: `PublicBacklog` 197 lines with 26 lines of private helpers. After: shared, and both views
call the same code.

## Red before green

`6 failed` on the three new cases across both projects. The failure output carried the defect
verbatim, which is why it is worth recording:

```
unexpected value "IN_PROGRESSStatus changed1.4.2026, 10:00:00From NEW to IN_PROGRESS..."
```

The German date appearing in a Chromium run is the locale dependence proving itself. After the fix:
`6 passed`, and the round 63 spec stays green (`20 passed` across both files together).

## Three wrong turns, recorded because the first diagnosis was wrong twice

The first two red runs were **not** a rendering bug. `AuthGuard` redirects to `/login`, and the
page was landing there every time. Three causes, found in order:

1. The seeded store omitted `isHydrated`. The repo already had the correct shape in
   `community-map.spec.ts`; I had not read it first.
2. `auth/me` reached the live backend, 401'd, and `axios.ts:79` called `logout()`.
3. The actual culprit: **`communities/my`**, fired by the shared `Layout` on every authenticated
   page. Unmocked, it 401'd, `auth/refresh` returned 403, and `logout()` wiped the seeded store.
   `signals/:id/comments` was unmocked too.

Lesson recorded in the spec's comment, because it is invisible from the view: on any authenticated
screen, an unmocked request is not a harmless gap. The response interceptor turns it into a logout.

## Also fixed: the capture script could photograph the wrong app

Round 63's script defaulted `BASE_URL` to port 5199, which belongs to another project, and mocked
everything it needed so it saved a stranger's UI as backlog evidence. `capture-signal-detail.mjs`
now defaults to the civic dev port and refuses to write anything unless the detail screen actually
rendered.

The `-lower` frame scrolls the timeline into view rather than to the bottom of the page. Scrolling
to `scrollHeight` on this screen framed the footer and skipped the audit trail entirely, which is
the one part these captures exist to review.

## Reproducing

```powershell
npm --prefix apps/web-react run dev -- --port 5299 --strictPort
$env:BASE_URL="http://127.0.0.1:5299"
node scripts/capture-signal-detail.mjs docs/evidence/round-64
$env:BASE_URL="http://127.0.0.1:5299"
npx --prefix apps/web-react playwright test src/tests/signal-detail-lifecycle.spec.ts
```

No backend required: routes are mocked and the session is seeded in `localStorage`.

## States captured

| Shot | What it shows |
| --- | --- |
| `in-progress-desktop.png` | Header badge "In progress", amber progress severity |
| `in-progress-desktop-lower.png` | The audit trail: "New" / "liaison" / "In progress", "From New to In progress", three `2026-04-01 10:00` stamps |
| `in-progress-mobile.png` | 390px, no overflow |
| `resolved-desktop.png` | Header badge "Resolved", green severity, proving the label change did not disturb severity mapping |

## Known limitations

- **The other 22 `toLocaleString` sites are untouched.** The primitive exists; sweeping it is its
  own round with its own tests. `CivicEngagement` (comment timestamps) and `CommunityThreads` are
  the ones a resident is most likely to read.
- **Pre-existing copy defects on this screen, found here and deliberately not fixed**, because they
  are an IA problem and not this round's objective:
  - the page eyebrow reads `PRIORITY RANK`, which names a card further down rather than the screen;
  - the subtitle under the title is `Intelligence Context`, the same words as the card directly
    below it;
  - the shared `Layout` shows the breadcrumb `Home` on a detail route.
  This is the same class the digest review found in round 60, so it is queued as one round.
- The lifecycle buttons still read "Mark IN PROGRESS". Those are action labels rather than a
  rendered enum, and `AGENTS.md`'s rule targets raw identifiers, so they were left alone. They are
  now inconsistently cased against the badge beside them.
- `signal-detail-timeline.spec.ts` cannot pass without a seeded backend: it logs in through the real
  form as `admin` / `admin12345`, and this database has no such account. It fails locally for that
  reason alone and is untouched here. It also navigates to `/signals/:id`, which is not a route -
  the route is `/signal/:id`.