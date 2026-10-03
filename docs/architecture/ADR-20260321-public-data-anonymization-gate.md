# ADR-20260321: Public Data Anonymization Gate

- **Status:** Accepted
- **Date:** 2026-03-21
- **Decides:** GitHub #30, closes it
- **Contract:** `packages/contracts/openapi.yaml` (`/api/community/exports/anonymization-check`)

## Context

AGENTS.md requires an anonymization step before publishing public dashboards. Until now that
rule had no enforcement behind it, and the open-data export path had a subtler problem than a
missing step.

The export records are structural. `OpenDataSignalRecordResponse` has no name, username, or
email column, so nobody's account details were leaking through the schema. What does leak is
free text a resident wrote, because residents ask to be contacted about their own report:

> "Streetlight out on Calle 12, ask for maria.lopez@example.org or call +34 600 11 22 33"

That sentence goes into a proposal's `proposedSolution`, and the export publishes it verbatim
to anyone holding a scoped API token. The failure mode is not a database leak. It is a
citizen's contact details leaving the platform because the platform had nowhere to put them
and no reason to notice.

## Decision

### Redact at the publish boundary, deterministic and un-pseudonymised

`PublicDataAnonymizer` runs on free-text fields of every public dataset: signal `title` and
`locationLabel`, proposal `title`, `problemStatement`, `proposedSolution`, `estimatedCost`,
`beneficiariesSummary`, and each entry in `supportingLinks`.

Three properties are non-negotiable:

- **Deterministic.** The same community exported twice yields identical bytes. A published
  dataset that changes between downloads cannot be cited by the people who rely on it.
- **No pseudonymisation.** A stable hash of a phone number is reversible by brute force over
  a tiny numeric space. Replacing with `[phone redacted]` is honest; hashing and calling it
  anonymised is not, and this project's whole premise is that the difference matters.
- **Visible.** The marker appears in the output. Redaction nobody can distinguish from data
  loss is its own transparency failure.

### Narrow patterns, because a false positive destroys real civic content

This is the tradeoff worth stating plainly. Detected: email addresses, phone numbers (9-15
digits), national ids (8 digits plus a trailing letter, DNI/NIE style), payment cards (13-19
digits).

Deliberately **not** detected: any bare 8-to-12 digit run. A budget line reading "the 2024
budget was 12,000,000 euros" must survive publication intact. That is why the national-id
pattern requires a trailing letter, and why the card pattern requires 13 digits. Widening
either is a product decision with civic consequences, not a threshold tweak.

Pinned by `shouldLeaveCivicProseIntact` and `shouldLeavePlainSignalReportsIntact`.

### The gate is on token minting, not on download

`GET /api/community/exports/anonymization-check` returns the checklist. Per field, not per
dataset: "the proposals dataset has unresolved findings" is not actionable, while
"PROPOSALS.proposedSolution: 3 record(s) with EMAIL" tells a steward exactly which text to
fix.

`POST /api/community/exports/tokens` is refused with `400` while `publishable` is false. That
is the choke point: it is how a community makes data readable by the public API, so it is
where an unchecked publish becomes impossible. Downloads need no extra check because the
redactor already ran.

### The escape hatch is explicit

`acknowledgeResidualRisk: true` proceeds. A community may legitimately want to publish a
contact address: a proposal whose whole point is "email the utility at this address". A tool
that cannot read intent should not pretend to. What it must do is make the decision recorded
rather than implied, which is what the flag is.

## Consequences

- Public open data no longer carries resident contact details.
- A data steward sees exactly which field needs editing before publishing.
- Publishing is refused by default rather than happening by omission.
- Over-redaction risk is bounded and pinned by tests; known blind spots are real names in
  prose ("Maria from number 12 asked me to report this"), which pattern matching cannot catch.
- The checklist is a per-request scan, not a cached index. Fine at community scale; revisit
  if a community reaches tens of thousands of records.

## Known limits

- Free-text names are not detected. A regex cannot reliably tell a resident's name from
  ordinary prose.
- Redaction is applied on export, so the source record keeps the original. That is required
  for the audit trail and is a deliberate trade: the platform stores what the resident wrote
  and publishes a redacted view.
- No retention policy is defined here. That belongs with the privacy work, not this gate.