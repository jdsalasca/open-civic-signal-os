# Round 65 evidence

Deliverable: no view in the frontend renders a timestamp through the reader's browser locale, and a
test now fails if anyone adds one back.

## The gap

Rounds 63 and 64 fixed this defect twice, one screen at a time, by eye. A grep found the rest:

```
24 matches for toLocaleString | toLocaleDateString | toLocaleTimeString
across 15 files
```

25 real call sites once comments were excluded. Every one of them had the same two faults:

- the same record read differently by two residents (`4/1/2026` against `1.4.2026`, and the ambiguous
  order can be either 1 April or 4 January);
- seconds on the screen for a source `LocalDateTime` that has none.

`toLocaleTimeString` was the worst of the three. With no locale argument it **drops the meridiem**,
so 6 PM renders as `18:00` in one browser and `6:00` in the next. That is not a time.

## What changed

Three primitives in `src/utils/formatStamp.ts`, replacing all 25 call sites:

| Export | Output | Replaces |
| --- | --- | --- |
| `formatDate` | `YYYY-MM-DD` | `toLocaleDateString()` |
| `formatTime` | `HH:mm` | `toLocaleTimeString()` |
| `formatStamp` | `YYYY-MM-DD HH:mm` | `toLocaleString()` |

`formatDate` exists because eight of the sites show a calendar day and no time; swapping
`toLocaleDateString` for `formatStamp` would have invented an hour those screens never displayed.
`formatTime` covers the blog card, the only place the app renders a date and a time as two spans.

Two files already had a local helper called `formatDate`, so the import is aliased to `formatDay`
there rather than shadowed.

All three return `undefined` for missing or unparseable input, so callers keep their own `"-"` and
`filter(Boolean)` behaviour. That also fixes a latent bug on the way: `new Date(undefined)` rendered
the literal string `Invalid Date` on screen, and no longer can.

## The guard

`src/tests/no-locale-timestamps.spec.ts` walks `src/`, strips comments and fails listing every
offending `file:line`. It is the point of this round: rounds 63 and 64 found the defect by eye across
25 sites, and an assertion per screen cannot stop the twenty-sixth. This can.

Comments are stripped before scanning, but newlines are preserved so a reported line number still
matches the file a developer will open. The first version of the test did not do that and pointed at
shifted lines, which is worse than useless on a 16-file sweep.

`src/tests` is excluded on purpose. An assertion about how a locale renders is legitimate there, and
the rule being enforced is "no locale timestamps in the UI", not "never name the method".

### It can actually fail

A green check that cannot fail is decoration, so the regression was injected and reverted:

```
# with new Date(message.createdAt).toLocaleString() restored
Error: Use formatDate, formatTime or formatStamp ... Offenders:
  views/CommunityThreads.tsx:284
1 failed

# reverted
1 passed
```

The line number in that failure is the real one.

## No regression, proved by comparison rather than assertion

The 18 specs covering the views touched here report **26 failed / 10 passed**. That number looks
alarming, so it was measured twice: once with this round's changes stashed, once with them applied.

| | failed | passed |
| --- | --- | --- |
| baseline (round 64, changes stashed) | 26 | 10 |
| with the 25-site sweep | 26 | 10 |

Identical. The failures are pre-existing and share one cause: those specs log in through the real form
as `admin` / `admin12345`, and this database has no such account, exactly like
`signal-detail-timeline.spec.ts`. `weekly-digest.spec.ts` is the clearest proof that they are not
mine: it exercises a view this round never touched and fails anyway.

No test was weakened, skipped or deleted to reach that number.

## What was verified visually, and what was not

The signal detail capture gained a seeded comment, because the engagement panel is the densest place
a resident reads someone else's timestamp. `in-progress-desktop-lower.png` and
`in-progress-mobile-lower.png` show the comment stamped `2026-04-01 10:00` at 1440px and 390px, with
no overflow on the narrow frame.

**Not visually verified: `formatDate` and `formatTime`.** They are covered by the guard and by the
no-regression comparison above, but no screenshot shows them. A capture for the blog card was built
and abandoned: the view's `timelineLoadInFlightRef` guard never populated the list under a mocked
boot, and the remaining value was two adjacent date spans that cannot plausibly render wrong. Shipping
a fragile script to photograph that would have been the more expensive mistake. Stating the gap beats
papering over it.

The capture script's failure error was improved instead of deleted, because it is what made the
abandonment a decision rather than a guess: it now reports the URL it landed on, the `main` text, and
every 4xx it saw.

## Reproducing

```powershell
npm --prefix apps/web-react run dev -- --port 5299 --strictPort
$env:BASE_URL="http://127.0.0.1:5299"
node scripts/capture-signal-detail.mjs docs/evidence/round-65
$env:BASE_URL="http://127.0.0.1:5299"
npx --prefix apps/web-react playwright test src/tests/no-locale-timestamps.spec.ts
```

## Known limitations

- `tsconfig.json` now includes `node` in `types`. `@types/node` was already a declared devDependency;
  it was simply not wired in, so a spec that reads the filesystem could not typecheck. The cost is
  that Node globals are visible to browser code in the editor.
- Notification times now read `18:00` in both languages. That is deliberate and is the fix for the
  meridiem loss, but an en-US reader may expect `6:00 PM`. Uniformity across the audit trail was
  judged more valuable than local convention; it is a judgement call, not a fact.
- 26 specs remain red for the seeded-backend reason above. They are unrelated to this round and were
  left alone rather than quietly mocked.