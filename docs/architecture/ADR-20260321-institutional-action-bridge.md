# ADR-20260321: Institutional Action Bridge

- **Status:** Accepted
- **Date:** 2026-03-21
- **Decides:** GitHub #10
- **Contract:** `packages/contracts/openapi.yaml` (`/api/community/institutional-handoffs`)

## Context

The municipal ticket export from #29 produces the file a city ingests. What it does not do is
answer the question a community asks next: **did anything happen to it?**

The honest constraint shapes the whole design: **the platform cannot read a city helpdesk.** It has
no integration with the institution's queue, and it should not pretend to. A "synchronised" badge
that meant "we guessed" would be worse than no badge, because a community would act on it.

## Decision

### Every status is declared or derived, and each handoff says which

`statusSource` is one of three values, and there is no fourth:

| Value | Meaning |
| --- | --- |
| `NO_INSTITUTIONAL_STATUS` | Nothing has been recorded. The clock is running and nobody has said anything. |
| `DECLARED_BY_COMMUNITY` | A person recorded that the city acknowledged or resolved it. A claim, with an author and a time. |
| `DERIVED_FROM_SIGNAL_LIFECYCLE` | The community's own signal was resolved, so the handoff is treated as resolved. |

The third is the interesting one. If the community resolved the signal, the resident's problem is
dealt with, which is what the handoff was for. But the platform derived that from its own data, not
from the city, and the record says so rather than presenting it as institutional confirmation.

There is deliberately **no field that pretends to be a live view of the institution's queue**.

### The SLA clock is the community's own, captured at handoff

`slaTargetDays` is stored on the handoff rather than read from current settings. A community
tightening its target next month must not retroactively turn a past on-time handoff into a late one.

`CLOSED_LATE` is judged against the moment it closed, not against today. A handoff that closed on
day 40 with a 30-day target was late; it does not become later every day.

### The interpretation says the platform cannot see the city

Every summary carries it: an overdue handoff means **the community's own target has passed, not that
the city has failed**, because the platform cannot see the city's queue.

Without that sentence, "3 overdue" reads as an accusation the platform is not in a position to make.

### One open handoff per signal and reference

A unique index on `(signal_id, ticket_ref)`. Two clocks for one complaint means nobody can say which
one was late.

### A handoff needs a ticket reference and a service code

Both are required, with the reason in the rejection:

- Without a reference, the handoff cannot be matched to anything the institution says back.
- Without a service code, it cannot be checked against what the city actually filed it as.

### A signal from another community cannot be handed off

Rejected with the reason: attributing a report to the wrong place is worse than refusing.

### Resolving implies acknowledging

Recording a resolution sets `acknowledgedAt` if it was empty. Leaving it empty would make the record
say the city never picked the ticket up and then closed it.

### Gated on the open-data management scope

Handing a resident's report to a city is an act with consequences for that resident, not a read.

## Consequences

- A community can track what it sent and what it learned, without the platform inventing data.
- Every status is attributable to a person or to a named derivation.
- The SLA clock cannot be rewritten by a later settings change.
- A community cannot accidentally create two clocks for one complaint.
- The platform never claims to know something about a city it cannot observe.

## Known limits

- **No UI.** The endpoints exist and are tested; there is no bridge screen. That is the remaining
  half of this issue.
- **No automatic sync.** Nothing polls a city system, because there is nothing to poll. Every status
  change is a person recording what they learned.
- **No reminder when a handoff goes overdue.** The summary reports it; nothing tells anyone.
- **`externalTicketId` is optional and unverified.** The platform records what it is told and cannot
  check it against the city.
- **No per-institution grouping.** Handoffs are per community; a community dealing with three
  departments cannot see them separately.
- **The export and the handoff are not linked.** A community can export a ticket and never record the
  handoff, or record a handoff for a signal it never exported. Nothing enforces the pair.
- **`DERIVED_FROM_SIGNAL_LIFECYCLE` can be wrong.** A community may resolve its own signal for its own
  reasons while the city ticket is still open. The source field is the mitigation: a reader can see
  the platform inferred it rather than being told.

## Batch handoff

Added after the single-signal version, because the export from #29 is bulk and recording handoffs one
at a time meant a community sending fifty tickets recorded fifty handoffs by hand.

`POST /api/community/institutional-handoffs/batch` takes a list of signals, one category map, and one
SLA target.

**Partial success is the expected outcome and is reported per signal.** A batch where three of fifty
were already handed off records forty-seven and says which three were skipped, rather than failing
entirely and leaving the community to work out why. Each outcome carries either the ticket reference
or the reason it was skipped.

**The ticket reference is derived from the signal id, the same way the export derives it.** Both now
call `InstitutionalTicketReference.forSignal`. Before that they computed it separately, and a
divergence would have meant a community handing over a ticket under one reference and then being
unable to match anything the city said about it.

**An unmapped category is skipped and named**, never defaulted, for the same reason the export
excludes it: a handoff filed against the wrong service code reaches the wrong department.

**At most 200 signals, rejected rather than truncated**, so a failure is attributable to a smaller
set and nobody compares a partial batch believing it was complete.

**Re-running the same batch skips rather than duplicates**, because the unique index on
`(signal_id, ticket_ref)` already refuses a second clock for the same complaint. A retry is therefore
safe, which is what makes a bulk operation usable.

### Batch limits

- **No per-signal SLA target.** One target for the whole batch. A community with different targets per
  department would need to split the batch.
- **No per-signal note.** One note for the batch, which is usually what a handoff run wants anyway.
- **No progress reporting.** A 200-signal batch is one synchronous request. Fine at community scale;
  a queue is the upgrade path the connector layer already names.
- **The batch is not transactional per signal.** A failure partway through leaves the earlier
  handoffs recorded, which is the intended behaviour — they were genuinely sent — but it means a
  caller cannot assume all-or-nothing.