# ADR-20260321: Municipal Ticket Export Adapter

- **Status:** Accepted
- **Date:** 2026-03-21
- **Decides:** GitHub #29
- **Contract:** `packages/contracts/openapi.yaml` (`/api/community/exports/municipal-tickets`)

## Context

A generic CSV export already exists. The obvious read is "this is a translation problem, tell
municipalities to write a parser and call it done".

That is the read that produces a bad handover. A city helpdesk runs fixed service codes, and
the platform's categories are free text written by residents. Somebody has to translate between
them, and if that somebody is a regex then a citizen's complaint about a water main gets filed
against the roads department and quietly stops being chased.

Two concrete risks drove the design:

1. **A report routed to the wrong department** looks like success. There is no error, the ticket
   exists, and the resident is never told. It is invisible from inside the platform.
2. **Contact details leaving the platform.** A municipal export lands in a procurement system
   with its own access rules, none of which this project controls. The same free-text PII
   problem as #30 applies, and now the destination is further away.

## Decision

### Publish the field map, and treat it as a contract

`GET /api/community/exports/municipal-tickets/field-map` returns every column with its type and
meaning, and the same map is embedded in every export response.

Municipalities pin importers to this. Adding a column is therefore a **breaking change** to
their parser and goes through an ADR rather than a convenience edit. The field map being in the
export response means a city can read what each column means without asking the platform owner
and discovering the answer changed last quarter.

### Category translation is community-owned, explicit, and per-request

`categoryMap` travels in the request body rather than being stored. The service codes belong to
one city's helpdesk; a stored mapping would keep applying after the city renames a service, and
the failure mode is a closed service code that quietly swallows every report filed under it.

### Unmapped categories are reported and excluded, never defaulted

This is the decision the whole ADR exists for. When a signal's category has no code:

- By default the ticket is **excluded** and the category is reported in `unmappedCategories`
  with `affectedReports`, so the community can see exactly what the gap is costing.
- With `includeUnmapped: true` the ticket is exported under an `UNMAPPED.<CATEGORY>` prefix,
  which is obviously-not-a-real-code, and the exclusion report still accompanies it.

The alternative was a default "OTHER" bucket. Rejected: it produces a valid-looking ticket in
the wrong department, which is the invisible failure this slice was meant to prevent.

`priority_score` is documented as **explicitly not** the city's own priority. A field named
like a priority that is not one will be treated as one.

### Redaction runs on this path too

`PublicDataAnonymizer` from #30 applies to title, description, and location. No author id, no
account, and no contact details appear in the ticket record at all.

Location is a **free-text label, never coordinates**. The precise location of a private home is
a safety risk, and publishing it to a municipal system is how it ends up on a leaked spreadsheet.

### Ticket references are deterministic

`ticket_ref` is `OCS-` plus a SHA-256 prefix of the signal id. Regenerating an export must not
invent new references, or the city loses their ticket history and every correspondence becomes
orphaned. Pinned by `ticketReferencesShouldBeStableAcrossRegenerations`.

### Provenance survives the handover

`source_channel` and `source_ref` travel with each ticket, so a report that arrived via the
bulk ingest importer can still be traced to `reports.csv#42` after the city has it.

## Consequences

- A city can build an importer from the published map without talking to anyone.
- Categories without a mapping are visible and counted, not silently misrouted.
- Column order and names are a contract; changing them needs an ADR.
- Contact details do not leave the platform on this path.
- A city cannot reconstruct the platform's internal identifiers for a signal, only the citation.

## Known limits

- JSON only. A CSV rendering of the same field map is the obvious next slice; the field map
  exists precisely so a renderer cannot invent its own columns.
- `affectedReports` counts reports across the community, not the city. A city scoping the
  export needs its own filter, which is a separate decision.
- No rate limit or delivery tracking on this path. The outbound integrations layer from
  `OCS-P1-053` is the right home for pushing tickets rather than polling for them.
- Manual export only. There is no sync loop, and adding one raises a real question about who is
  accountable when the platform and the city disagree about a report's status.