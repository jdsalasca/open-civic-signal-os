# Round 63 evidence

Deliverable: the public backlog speaks plain language, and its accountability claim does not change
shape with the reader's browser.

## The gap

`/backlog` is the one screen a resident can read without an account, so it is the screen where a
vocabulary mistake costs the most trust. It was publishing three kinds of machine output as if they
were citizen-facing text:

```
Data last updated 1/4/2026, 10:00:00 a. m.      Status: IN_PROGRESS      313.00 score
```

- `1/4/2026` is 4 January to a reader in one hemisphere and 1 April to another. On a page whose
  entire purpose is a verifiable claim, the date of that claim was ambiguous.
- `10:00:00 a. m.` implied seconds and a half-day clock. The source is a Java `LocalDateTime` with no
  offset and no seconds, so the screen was asserting precision the data does not have.
- `Status: IN_PROGRESS` is an API identifier. `AGENTS.md` forbids showing raw enums when a plain
  label exists.
- `313.00` gave the score two decimal places. The published formula is
  `(Urgency * 30) + (Impact * 25) + min(People/10, 30) + min(Votes/5, 15)`, whose terms are integers,
  so the trailing zeros were decoration.

The date was also **locale-dependent**: `toLocaleString()` with no argument renders from the
browser's default locale. The same page told a US reader `4/1/2026, 10:00:00 AM` and a German reader
`1.4.2026, 10:00:00`. Two residents were comparing two different-looking claims about the same data.

## What the reader is now told

```
Data last updated 2026-04-01 10:00      Status: In progress      313 score
```

Fixed `YYYY-MM-DD HH:mm`, rendered from local date components rather than a locale lookup. That keeps
the existing semantics - the `LocalDateTime` is still read as local time, no offset is invented - while
being unambiguous in English and Spanish and sorting correctly.

The locale dependence is now pinned by a test that runs the page under `locale: 'de-DE'`. It is the
strongest of the four, because before the fix it failed with the German string itself in the
assertion output:

```
Expected substring: "2026-04-01 10:00"
Received string:    "Data last updated 1.4.2026, 10:00:00"
```

## Red before green

`8 failed / 6 passed` on the four new cases across both projects, then `14 passed` after the fix. The
four failing cases are the three leaks above plus the locale variant.

## The capture script was pointing at someone else's app

The first capture of this round produced screenshots of a **different project**: port 5199 belongs to
`Universiry/frontend`, and `scripts/capture-public-backlog.mjs` defaulted `BASE_URL` to it. The script
mocked every API route it needed, so nothing errored; it just wrote a stranger's UI into this repo's
evidence folder.

Two changes, because a default that can silently capture the wrong app will do it again:

- the default is now `5173`, the civic dev frontend;
- the script now waits for `.public-backlog-shell` and throws before writing anything if it is absent,
  so a mis-pointed `BASE_URL` fails loudly instead of producing a convincing wrong screenshot.

## Reproducing

```powershell
docker compose -f infra/docker-compose.yml up -d civic-api   # optional; routes are mocked
npm --prefix apps/web-react run dev -- --port 5299 --strictPort
$env:BASE_URL="http://127.0.0.1:5299"
node scripts/capture-public-backlog.mjs docs/evidence/round-63
$env:BASE_URL="http://127.0.0.1:5299"
npx --prefix apps/web-react playwright test src/tests/public-backlog.spec.ts
```

The live backend is not required. `docs/LOCAL_VERIFICATION.md` records the house rule that UI evidence
mocks its API routes; the capture script follows it.

## States captured

| Shot | What it shows |
| --- | --- |
| `populated-desktop.png` / `-lower.png` | Three ranked items, published formula, freshness stamp |
| `populated-mobile.png` / `-lower.png` | 390px, no overflow, formula wraps, metadata stacks |
| `empty-desktop.png` / `-lower.png` | Zero signals, formula still visible so the method is auditable |
| `api-unavailable-desktop.png` / `-lower.png` | Unreachable API, no login prompt |

`api-unavailable` reports three `net::ERR_FAILED` console errors. Those are the aborted routes the
script creates on purpose; they are the condition being captured, not a defect.

## Known limitations

- When the API is unreachable the freshness badge reads "Data last updated not reported yet", which
  conflates "there is no data" with "we cannot reach the data". Deciding what a screen should claim
  about freshness when it is down is a product call, so it is left alone here rather than changed
  quietly inside a copy fix.
- Lifecycle labels are translated per locale. `SignalDetail` still renders `signal.status` raw in its
  badge; that view sits behind the same enum and deserves the same map in its own round.
- The score still has no stated direction. The formula card and the descending order make it
  derivable, but "higher ranks higher" is not written anywhere.