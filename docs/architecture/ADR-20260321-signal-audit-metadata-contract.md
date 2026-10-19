# ADR-20260321: Signal Audit Metadata And Transformation Version Contract

- Status: Accepted
- Date: 2026-03-21
- Closes: GitHub `#18`, `#41`
- Story: `OCS-P0-005`

## Context

The platform already publishes an explainability breakdown per signal, which answers "why is this ranked here". It did not answer the question an auditor asks next: "where did this record come from, and which scoring rule produced this number?"

Concretely, `Signal` had no `source_channel`, `source_ref`, or `transformation_version`. `SignalMetaResponse` carried only counts and a timestamp. The scoring formula lived as a hardcoded string in `TrustPacket.CURRENT_FORMULA` and a second copy in `PrioritizationServiceImpl.getBreakdown`, so the published formula and the code that computed the score could drift without anything detecting it.

This is the AGENTS.md rule "keep audit trail metadata: source, timestamp, transformation version" and "distinguish citizen reports from institutional updates using explicit fields" — both unmet.

## Decision

Attach audit metadata to the signal itself, so it travels with every ranked output:

- `source_channel` — enum `WEB_FORM`, `CSV_IMPORT`, `CHAT_EXPORT`, `PUBLIC_API`, `INSTITUTIONAL_UPDATE`. Non-null, defaulting to `WEB_FORM`.
- `source_ref` — nullable free-text pointer back to the original record (`rows/2026-03-24.csv#142`, `municipal-ticket/88213`).
- `transformation_version` — non-null, stamped by the prioritization service on write.

`PrioritizationServiceImpl` owns a single `TRANSFORMATION_VERSION` constant referenced by `Signal.TRANSFORMATION_VERSION_V1`, with a comment requiring a bump whenever the weights in `getBreakdown` change plus a regression note in `docs/`. That collapses the previous duplication between the published formula string and the computation.

Two decisions worth naming:

- **Unknown ingest channels are rejected, not defaulted.** `resolveSourceChannel` throws for an unrecognized value. Silently falling back to `WEB_FORM` would file a broken import as a citizen report, which is exactly the confusion the field exists to prevent.
- **Audit metadata is not separately exposed.** It rides on the existing `SignalResponse` rather than a new endpoint, so every surface that already renders a signal — detail, prioritized list, public exports — shows provenance without a second fetch.

`SignalDetail` renders a "Where this came from" panel with the human-readable channel, the source reference, and the scoring rule version.

## Consequences

Positive:

- a ranked number can be traced to an ingest channel and to the rule version that produced it
- a municipal ticket update is now distinguishable from a citizen report in the data, not just by convention
- the formula version and the weight computation are no longer two independent strings

Trade-offs:

- `source_ref` is free text. There is no referential integrity to the originating system, so a stale reference is possible and undetectable.
- `transformation_version` marks the version at write time. Re-scoring historical signals under a new rule does not backfill them; a rank always reports the rule that was in force when the score was stored. Backfilling would need an explicit re-scoring job and its own audit trail.
- the version is a plain string, so nothing enforces that it matches `TrustPacket.CURRENT_FORMULA`. A drift check between the two belongs in a release gate, which does not exist yet.
- `CHAT_EXPORT` and `PUBLIC_API` are declared but no ingest path sets them yet, because there is no chat-export parser and the public API does not create signals.

## Validation

- `SignalAuditMetadataIT` covers the default channel and version stamp, preservation of an explicit ingest channel through the prioritized list, institutional-update versus citizen-report distinguishability, and rejection of an unknown channel
- `SignalCreateRequest` and `SignalResponse` updated in the same change set as OpenAPI, including a new `SignalSourceChannel` schema
- Playwright: `signal-detail-provenance` test ids expose the channel and version panels

### Note on the quality gate

While validating this story the backend test suite turned out to be largely dead in CI: Surefire's default includes match `*Test`/`Test*`/`*Tests`/`*TestCase`, and this repo keeps its backend coverage in `*IT` classes. `mvn -q test`, which `ci.yml` runs, therefore executed 2 unit classes and 6 tests, leaving 36 integration classes unrun. Pinning `**/*IT.java` in the Surefire configuration took the executed count from 6 to 101.