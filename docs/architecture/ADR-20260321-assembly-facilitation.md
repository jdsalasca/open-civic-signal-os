# ADR-20260321: Assembly Facilitation

- **Status:** Accepted
- **Date:** 2026-03-21
- **Decides:** GitHub #49
- **Contract:** `packages/contracts/openapi.yaml` (`/api/community/assemblies/{id}/agenda`, `/facilitation`)

## Context

The assembly record from #27 captures what was deliberated and decided. That is the record half.

A townhall also needs the **facilitation** half: what the facilitator planned, in what order, and how
the room is tracking against it. Without a plan, a facilitator improvises, and the items at the end
of the agenda are the ones that get dropped.

## Decision

### The agenda has a running order and a time per item

`plannedMinutes` is required in spirit — it defaults to 10 when omitted — because a facilitator needs
a number to pace against, and "we will discuss it" is not a plan.

`runningOrder` reports the **cumulative** planned minutes to reach each item, so a facilitator can
see where they should be at any point rather than doing arithmetic in their head.

### The platform does not run the meeting

`interpretation` says so on every response, and `pacing` is a fact rather than a verdict:

| Value | Meaning |
| --- | --- |
| `NOT_STARTED` | The assembly is not open, so no elapsed time is tracked. |
| `NO_PLAN` | Open, but no agenda, so there is nothing to pace against. |
| `ON_PLAN` | Under 80% of the planned time. |
| `NEARING_END` | At or past 80%. |
| `OVERRUNNING` | Past the plan. |

**`OVERRUNNING` is not a failure.** A facilitator who gives an item twice its slot may be making the
right call about the room, and the platform has no standing to call it wrong. It reports, it does not
prevent.

### It does not reorder by score

The order is the facilitator's. A platform that reordered a townhall agenda by priority score would
be deciding what the community discusses, which is the opposite of what an assembly is for. The
interpretation states this explicitly.

### Setting an agenda replaces it

Not appending. A facilitator editing the plan mid-meeting would otherwise end up with two items at
the same position, and the unique index on `(assembly_id, position)` would refuse the write anyway.
A half-applied agenda is worse than a rejected one.

### The agenda cannot change after closing

Refused with the reason: changing the agenda afterwards would change what the record says was
planned. Pinned by `aClosedAssemblyShouldNotAcceptAnAgendaChange`.

### An item's subject is optional

Same reasoning as assembly decisions: a townhall discusses things that are not yet records, and
forcing a link would make people create placeholders.

### Limits are rejected, not truncated

At most 50 items, each at most 240 minutes. A longer agenda is a schedule, not a meeting, and a
truncated one would leave a facilitator believing the plan was complete.

## Consequences

- A facilitator has a plan to pace against, with cumulative timings.
- Overrunning is visible without being treated as a failure.
- The platform cannot reorder what a community discusses.
- The agenda cannot be rewritten after the meeting.
- An assembly with no agenda says so rather than reporting a meaningless zero.

## Known limits

- **No UI.** The endpoints exist and are tested; there is no facilitation screen. That is the
  remaining half of this issue.
- **No live item tracking.** The view reports elapsed time against the whole plan, not which item the
  room is currently on. A facilitator still marks their own place.
- **No speaker queue or timeboxing.** The platform does not track who is speaking or cut anyone off.
- **No attendance.** Same gap as the assembly record: the agenda does not say who is in the room.
- **`plannedMinutes` is not enforced.** It is a plan, and the platform reports against it rather than
  holding anyone to it.
- **No templates.** A community running a similar townhall every month re-enters the agenda each time.
- **No per-item actuals.** The record does not capture how long each item actually took, so a
  facilitator cannot compare plan to reality afterwards. That would need a per-item start and end,
  which is a different model.
- **The pacing thresholds (80%) are constants**, not per-community.