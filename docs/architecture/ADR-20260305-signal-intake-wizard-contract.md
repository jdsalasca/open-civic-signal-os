# ADR-20260305: Signal Intake Wizard Contract

- Date: 2026-03-05
- Status: Accepted

## Context

Signal intake previously captured only free text, one optional image URL, and raw coordinates. That was not enough for institutions to route work quickly, and it forced residents to guess what information mattered.

`story:OCS-P1-037` requires intake to behave more like an issue tracker: readable location context, optional multiple evidence links, and guided step-by-step capture.

## Decision

Extend signal create/read contracts with:

- `locationLabel`
- `evidenceUrls[]`

Keep `imageUrl` as the first/primary evidence reference for backward compatibility with older clients and existing detail surfaces.

Frontend moves to a 3-step wizard, but backend remains the owner of the payload and persistence shape.

## Consequences

Positive:

- case intake becomes more actionable for institutions
- people can describe location in community language, not only coordinates
- multiple evidence links improve verification without blocking low-friction submission
- older views that still read `imageUrl` keep working

Tradeoffs:

- signal payload becomes larger and requires tighter validation
- evidence is still URL-based for now, not direct file upload
- assignment and lifecycle auditing still depend on `OCS-P1-038`

## Validation

- backend create contract integration test for `locationLabel` and `evidenceUrls`
- frontend Docker build against the new wizard and signal detail shape
- Playwright wizard flow updated for the new multi-step experience
