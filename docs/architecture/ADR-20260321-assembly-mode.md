# ADR-20260321: Assembly Mode

- **Status:** Accepted
- **Date:** 2026-03-21
- **Decides:** GitHub #27
- **Contract:** `packages/contracts/openapi.yaml` (`/api/community/assemblies`)

## Context

The pieces existed separately: explainability snapshots freeze what an assembly saw, the trust pulse
records what residents think, and the backlog publication makes a ranking checkable. What was missing
was the **session** that ties them together — what was deliberated, over which evidence, and what was
decided.

## Decision

### It is not the official minutes, and says so on every read

The platform records what happened in the room. The authoritative record remains whatever the
community is legally required to keep.

A platform that presented its own record as the minutes would be claiming an authority it does not
have. `interpretation` states this on every response, and the `minutes` column is documented as free
text the platform does not treat as official.

### It does not decide anything

There is no route that approves a proposal on the community's behalf. `POST /decisions` records a
decision people made, with the person and the time attached.

Same principle as the policy sandbox and the participatory budget: a tool that made the choice would
be making a value judgement silently.

### Evidence is attached separately, and a missing snapshot is reported

`POST /{id}/evidence` links the frozen ranking. It is separate from creation because the evidence is
usually captured closer to the meeting, and forcing it at creation would make people create a
placeholder snapshot. **A placeholder in an evidence chain is worse than an honest gap.**

`evidenceCaptured` is false until a snapshot is attached, and the interpretation says the ranking
"cannot be reproduced" and that "a decision made over evidence nobody can produce is a decision
nobody can review". Pinned by `creatingAnAssemblyShouldRecordItWithoutEvidence`.

### Evidence cannot be attached after closing

Refused with the reason: attaching evidence afterwards would change what the record says was
deliberated over. Pinned by `evidenceAfterClosingShouldBeRefused`.

### A decision after closing is refused

A decision recorded afterwards would appear in the record as having been made in the room. Pinned by
`aDecisionAfterClosingShouldBeRefused`.

### Closing twice is refused

Reopening would erase the record of when it ended. Pinned by `closingTwiceShouldBeRefused`.

### A rationale is required

A decision nobody explained cannot be reviewed afterwards, which is the whole reason for recording
it. The rejection says so.

### The subject is optional

`subjectId` is nullable because an assembly can decide something not yet in the system. Forcing a link
would make people create placeholder records to satisfy the schema, and a placeholder in a decision
log is worse than an honest "this was about something not in the system".

### A snapshot from another community cannot be attached

The lookup is scoped to the community, so a cross-community link is a `404` rather than a silent
mismatch.

## Consequences

- A community can record what it deliberated and decided, with the evidence linked.
- The record never claims to be the official minutes.
- A decision made without captured evidence is flagged rather than presented as reviewable.
- The record cannot be rewritten after the fact: no late evidence, no late decisions, no reopening.
- Every decision carries a rationale and an author.

## Known limits

- **No UI.** The endpoints exist and are tested; there is no assembly screen. That is the remaining
  half of this issue.
- **No attendance or quorum.** The record does not say who was present or whether the meeting was
  quorate, which is often the first thing a challenge turns on.
- **No voting.** Decisions are recorded, not tallied. Linking an assembly decision to the proposal
  voting from `OCS-P1-042` is a separate decision about what a vote means for an assembly.
- **No agenda.** There is no ordered list of items, so the record does not show what was discussed
  versus what was decided.
- **`minutes` is free text with no structure.** A community wanting structured minutes needs a
  different model.
- **No notification.** Nothing tells members an assembly is scheduled.
- **No recurring assemblies.** Each one is created separately.
- **The evidence link is one snapshot.** An assembly that deliberates over several rankings would need
  a list, and the current model assumes one.
- **`CANCELLED` exists in the enum but no route sets it.** A cancelled assembly can only be created
  and left scheduled, which is an inconsistency rather than a feature.