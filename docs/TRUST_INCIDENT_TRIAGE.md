# Trust-Critical Incident Triage

A trust-critical incident is any event that could cause a resident, a community organisation, or an
institution to act on a number this platform produced that was wrong.

The distinction matters. A broken page is an outage. A **wrong number presented as auditable** is a
trust incident, because the whole premise of this project is that its figures can be checked. An
incident that is quietly corrected has done more damage than the error itself.

- **Related:** the label conventions in `AGENTS.md` (`trust-critical`, `data-quality`)
- **Evidence capture:** `npm run incident:capture -- --label "<short description>"`
- **Decision records:** `docs/architecture/decision-index.md`

## What counts

Treat as trust-critical when **any** of these is true:

| Trigger | Why it is trust-critical |
| --- | --- |
| A published priority score or rank order is wrong | Residents and institutions act on the order |
| Two reports were merged that were not the same problem | A resident's complaint stopped being counted |
| Personal data appeared in public open data | A resident is exposed by the platform's own transparency feature |
| A digest or bulletin was sent with the wrong community's or wrong week's figures | It reaches people directly and is hard to retract |
| A municipal ticket export was filed against the wrong service code in bulk | Complaints reach the wrong department and stop being chased |
| The published formula does not match what the running code computes | Every "why is this ranked here" explanation is then false |
| A surface has been stale or silent and nobody knows since when | Decisions are being made on a frozen view |

## What does **not** count

Escalate these as ordinary bugs, or the protocol stops meaning anything:

- A page that fails to load
- A slow query or a timeout
- An unused or cosmetic UI defect
- A single report filed with the wrong category by its own author
- A rejected report that the reporter disagrees with, where the reason was recorded

## Severity

| Level | Definition | Examples |
| --- | --- | --- |
| **S1** | Personal data exposed publicly, or figures already acted on by an institution | PII in a published export; a county decision citing a wrong rank |
| **S2** | Wrong figures published and reachable, not yet known to have been acted on | A miscomputed priority list on `/backlog`; a bad digest delivered |
| **S3** | Wrong figures possible, or a control that should have caught something did not | Formula drift detected; a gate that passed something it should not have |
| **S4** | Trust-relevant weakness with no wrong output yet | No rate limit on the collector; a test that cannot fail |

## First 15 minutes

Do these **before** investigating, because the evidence degrades:

1. **Capture the bundle.** `npm run incident:capture -- --label "<short description>"`. It records
   the formula version, the current ranking hash, freshness per surface, and counts. Without it,
   "what did the ranking look like when the complaint arrived" is unanswerable in an hour.
2. **Do not fix anything yet.** A fix changes the state the bundle was meant to preserve. If urgent
   containment is needed, capture first, then contain.
3. **Write down how it was noticed.** Who or what reported it, at what time, and through which
   channel. This is the only record of whether the platform's own monitoring works.
4. **Name a single incident owner.** Not a team. One person accountable for the next step.

## Containment, by class

| Class | Containment |
| --- | --- |
| PII in published data | Revoke the open-data token, then re-publish. The export gate exists for this; a token that leaked data was minted before the gate saw the finding |
| Wrong ranking | Do not silently rescore. Regenerate the reproducibility capture so the difference is a reviewable artefact, then correct forward |
| Bad merge | Record a `SPLIT` decision against the merge. Note: un-merge is not implemented, so a split is currently a declaration — say so plainly rather than implying the reports came back |
| Bad digest | Do not resend. The week is locked by design; send a correction as the *next* week's digest with the error stated |
| Wrong municipal codes | Stop the export for that community. The category map is per-request, so the fix is a corrected map, not a data migration |
| Formula drift | Publish a new formula version with a `changeNote`. Never edit a published version in place; the version is what makes old snapshots interpretable |

## Communication rule

**A correction must be at least as visible as the error was.**

If a wrong number appeared on the public backlog, the correction appears there, with the affected
period named. If a digest was delivered, the correction is delivered through the same channel. A
correction buried in a changelog while the error sat on a public page is a second, quieter failure.

What to say: what was wrong, for which period, who could have seen it, what has been corrected, and
what a reader should do if they already acted on it.

What not to say: "a minor data issue", "some users may have experienced", or anything that
describes the correction as smaller than the error.

## Who decides

| Decision | Owner |
| --- | --- |
| Declaring a trust-critical incident | Any maintainer, unilaterally. The cost of a false declaration is small |
| Containment that removes public data | The incident owner, after capture |
| Publishing a correction to a community | The incident owner, and it is not optional once S1 or S2 |
| Publishing a new formula version | Requires an ADR, per the existing governance rule |
| Closing the incident | The incident owner, with the written record merged |

## Aftermath

Within a week of closing, in the same repository:

1. **A written record** in `docs/architecture/` following the ADR format: what happened, how it was
   detected, what was corrected, and what would have caught it earlier.
2. **A guard, or an explicit statement of why none is possible.** The pattern in this repository is
   that every incident so far produced one: a parity gate from a missed route, a reproducibility
   check from a formula copy, a publish gate from a public-export gap, and a mutation-verified test
   from a guard that could not fail.
3. **A statement of the residual risk.** Some incidents cannot be guarded against — missing
   geography, absent demographic data, a hash with no external anchor. Naming them is the honest
   outcome, and it is preferable to a guard that appears to cover them.

## What this platform cannot yet do

Stated here rather than implied, because a protocol that assumed these existed would be fiction:

- **No alerting transport.** Freshness monitoring reports; nothing pages anyone. Detection depends on
  a person noticing.
- **No on-call rotation.** Response is best-effort by whoever is available.
- **No external hash anchor.** If a database row and its recorded hash were both altered, snapshot
  verification passes. Detecting that needs a published digest or a signature.
- **No audit of reads.** Open-data access is logged; authenticated dashboard reads are not.
- **No automatic rollback.** Corrections are forward-only by design, so a wrong figure is superseded
  rather than erased. That is deliberate — erasure would destroy the record that the error happened.
