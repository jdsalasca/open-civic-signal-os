# ADR-20260321: Explainability Snapshots For Assemblies

- **Status:** Accepted
- **Date:** 2026-03-21
- **Decides:** GitHub #52
- **Contract:** `packages/contracts/openapi.yaml` (`/api/community/explainability-snapshots`)

## Context

The platform already has a trust packet: score, breakdown, formula string, and a per-row
verification hash. On its own that is not enough for a deliberation.

An assembly ranks a list on Tuesday and votes on Wednesday. The record of what they saw has to
still be checkable in six months. But a live score moves: one more vote changes it, one status
change reorders the list. So "the list we voted on" quietly stops being a thing that exists, and
the minutes become a claim nobody can test.

## Decision

### A snapshot freezes the rows, not just the scores

`POST /api/community/explainability-snapshots` writes the ranked rows, the formula version, the
formula expression, and each entry's position and verification hash into an immutable record.

Immutable means no update and no delete route. A snapshot that can be edited is not evidence,
and an API that offers the verb has already lost the argument.

### Verification recomputes the hash; it does not regenerate the ranking

`GET /{id}/verification` recomputes the SHA-256 over the stored payload and compares it to the
recorded hash.

It deliberately does **not** regenerate the ranking from current data. Those answer different
questions: "were these rows altered?" versus "does the backlog still look like this?". The
second would report `valid` for a snapshot whose original ranking has since changed, which is not
tampering but is not what a reader of the minutes is asking. `explanation` says what the result
does and does not prove.

Both hashes are returned, not just a boolean, so a reader can compare them instead of accepting a
verdict from the same party that produced the data.

### The payload is canonicalised before hashing

Object keys sorted at every level; array order preserved.

Key sorting is what makes the hash stable across JVM versions, Jackson defaults, and any future
refactor of how the payload is built. Array order is *not* sorted, because entry order **is** the
ranking: sorting arrays would make a reordered ranking hash identically to the original.

Without canonicalisation a snapshot can fail verification for reasons that have nothing to do
with tampering, and readers learn to ignore the check — which defeats the entire mechanism.
Pinned by `hashShouldNotDependOnKeyOrdering` and `hashShouldPreserveEntryOrderInArrays`.

### A label is mandatory

An unlabelled snapshot is unfindable six months later. `label` is required, and the reason is in
the contract: "Assembly of 12 March, agenda item 4".

### Reading a snapshot requires membership

A snapshot is assembly evidence. Anyone authenticated could otherwise enumerate another
community's deliberation record by walking ids.

This was a real oversight in my first draft, which checked only that the community existed. The
membership repository is now wired in and `nonMemberShouldNotReadAssemblyEvidence` pins it.

### Community-scoped, not assembly-scoped

There is no assembly entity yet. A snapshot is community-scoped and an assembly will reference a
snapshot id. Building an assembly abstraction first would have been speculative; this is the
smallest thing that makes the minutes checkable.

## Consequences

- Meeting minutes can cite a snapshot id and a hash a reader can recompute.
- The ranking an assembly saw survives later votes and status changes.
- Tampering with stored rows is detectable and says so explicitly.
- Per-entry hashes let one disputed row be checked without re-deriving the snapshot.
- No way to correct a snapshot after creation. A mistake means creating a new one, which is the
  intended behaviour for an evidence artefact.

## Known limits

- No assembly entity, so snapshots are not yet linked to a meeting, an agenda item, or a quorum.
- No signature. The hash proves the rows were not altered after creation, but it does not prove
  *who* created it. `createdBy` is recorded but unauthenticated as authorship: a hash is not a
  signature, and the OpenAPI description says so rather than implying otherwise.
- The hash has no external anchor. If the database row and the recorded hash were both altered,
  verification passes. Detecting that needs an off-platform anchor: a published digest, a
  transparency log, or a signature.
- Votes are read at snapshot time. An assembly that votes after capturing gets the ranking as of
  capture, which is the intent, but nothing warns them that the ranking moved since.
- Limit is capped at 200. A larger freeze needs pagination or an export artefact rather than a
  bigger payload.