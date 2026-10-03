# ADR-20260321: Bulk Ingest Contract With Row-Level Validation

- **Status:** Accepted
- **Date:** 2026-03-21
- **Deciders:** civic platform maintainers
- **Closes:** GitHub #12, GitHub #13
- **Contract:** `packages/contracts/openapi.yaml` (`/api/ingest/validate`, `/api/ingest/commit`)

## Context

Communities that already collect reports elsewhere arrive with a file, not with a blank
form: a spreadsheet maintained by a neighbourhood association, or a WhatsApp group export.
Two failures made the old path unusable:

1. The CSV reader stopped at the first malformed row and discarded everything after it, so
   one typo in row 400 cost the steward 399 good reports and a manual re-export.
2. The chat parser accepted anything with a text-ish field, so a line of `ok` or `👍` became
   a ranked signal in a public backlog. That is exactly the black-box ingest this project
   exists to avoid.

Anchoring on real community workflows also means the raw file must survive the import. If a
scoring formula changes in six months, the only way to replay it is to keep the original
input and cite it.

## Decision

### Two routes, one report shape

`POST /api/ingest/validate` is a dry run that writes nothing. `POST /api/ingest/commit`
persists only the rows that passed validation. Both return the identical `IngestReport`, so
a client renders one table and flips a button.

The `commit` flag in the request body is ignored on both routes. Keeping the field lets one
client payload drive both calls, but the route decides the behaviour, not a body field that
a caller could flip by mistake.

### Row-level errors, never a whole-file failure

Every row produces an `IngestRowOutcome` with `valid` and its own `errors` array. A rejected
row names the column and an actionable `code` (`REQUIRED`, `OUT_OF_RANGE`, `NOT_A_NUMBER`,
`COLUMN_COUNT_MISMATCH`, ...) with a truncated `preview`, never the full row. The steward can
fix row 12 without re-reading row 1.

The flat `errors` array is the same set of errors, capped for display. `errorsTruncated`
says when that cap was hit. Per-row lists stay complete regardless.

### Ragged rows are rejected, not shifted

A row whose column count differs from the header is rejected with `COLUMN_COUNT_MISMATCH`
before any value is mapped.

This one is deliberate and worth stating, because the cheaper implementation is worse.
Padding and truncating to the header length silently moves every later value into the wrong
column. A `infrastructure` category lands under `urgency` and, when the value happens to be
numeric, the row imports as valid with the wrong meaning. Rejecting is the only option that
keeps the import trustworthy. Caught while writing the tests for this ADR.

### Recognised sources fail loudly

`CSV_EXPORT`, `WHATSAPP_EXPORT`, and `TELEGRAM_EXPORT` each have an explicit reader. A chat
export with no recognised text column returns `400` rather than a zero-row success. An
unterminated quote returns `400` rather than a mis-parsed remainder.

### Raw input is preserved and cited

Accepted signals store `sourceChannel` (`CSV_IMPORT` / `CHAT_EXPORT`), `sourceRef` as
`fileName#line`, and `transformationVersion`. `contentSha256` on the report hashes the exact
bytes submitted, so the same file can be shown to have produced the same result.

Ingest stores the raw text unredacted, because the audit trail requires the original. The
anonymisation step for public dashboards runs on publish and is a separate concern
(GitHub #30); conflating the two would make the audit trail unciteable.

### Scope

Maximum 2000 rows per request, rejected with `409` before parsing. A community importing a
100k-row history should be batched deliberately rather than in one transaction that holds a
lock. Callers that legitimately need more should ask for streaming ingest rather than
getting an undocumented ceiling raised.

Permission scope `IMPORT_SIGNALS` gates both routes. It is independent of
`MANAGE_MODERATION_QUEUE`: importing is data entry, not adjudication, and a community may
reasonably allow a data steward to import without giving them sanction powers.

## Consequences

- One malformed row no longer costs an upload.
- Every imported signal is traceable to a file and a line.
- Rejected rows are visible with a reason a non-technical steward can act on.
- A chat line like `ok` cannot become a ranked public signal.
- The 2000-row ceiling is a real limit; batch uploads above it.
- Community admins gain a new policy switch they can revoke independently of moderation.

## Rejected Alternatives

- **Keep failing on the first bad row.** Cheapest, and it is the current behaviour. It is
  unusable at the volume a community association actually has.
- **Import everything, flag problems after.** Rejected: a known-bad row becomes a public
  signal. The audit trail would then record something we already knew was wrong.
- **Map columns by position with padding.** Rejected above; silent corruption is worse than
  a rejection.
- **One endpoint with a `dryRun` flag.** Rejected: the write path becomes reachable by a
  mistyped flag, and the permission boundary gets mushy. Two routes, one shape.