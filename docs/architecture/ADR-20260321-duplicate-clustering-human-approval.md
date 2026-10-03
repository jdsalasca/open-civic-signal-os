# ADR-20260321: Duplicate Clustering With Human Approval

- **Status:** Accepted
- **Date:** 2026-03-21
- **Decides:** GitHub #36, addresses the audit half of GitHub #8
- **Contract:** `packages/contracts/openapi.yaml` (`/api/signals/merge-review/*`)

## Context

Duplicate detection and a merge endpoint already existed. `findDuplicates` scores pairs by
normalised title token overlap with a containment shortcut, and `POST /api/signals/merge`
combines them.

What was missing was everything that makes such a feature safe to trust. A merged signal keeps a
`mergedFrom` list of ids, but nothing records who decided, what they were shown when they decided,
at what threshold, or whether they changed their mind. After the fact, a correct merge and a
wrong one look identical.

This matters more than usual here. Merging two reports is not a formatting decision: it silently
reduces the number of things an institution is visibly accountable for. A wrongly merged pair is a
resident whose complaint stopped being counted.

## Decision

### The suggestion is stored verbatim, never recomputed

The review request carries `suggestedSimilarities` from the client and that is what gets written
to `signal_merge_decisions`.

The obvious alternative, recomputing server-side on save, is worse. An algorithm tweak next month
would retroactively change what every past reviewer was shown, quietly rewriting the evidence for
decisions people already made. The record has to say what the person was actually looking at.

### One similarity definition, shared by the queue and the decision

`PrioritizationService.similarityScore` is now public and returns the same token-overlap number
`isSimilar` already gates on, with `1.0` for the identical and containment cases.

Without this the review queue would display a score while the merge applied a different rule, which
is worse than showing no score at all: a moderator approves a `0.8` the backend does not consider
a duplicate.

### Declining is a first-class outcome

`REJECTED` and `SPLIT` are recorded with the same fidelity as `APPROVED`, including the note. A
target whose suggestion was declined still appears in the queue carrying `latestDecision`, rather
than disappearing.

Two reasons. Hiding it makes the queue look permanently unfinished every time someone correctly
said "these are not the same", and invites the same suggestion to be re-decided next week. And a log
that only records approvals trains people to read it as a changelog instead of a record, at which
point they stop reading it.

### An approval that cannot be audited is refused

Two rejections, both `400`:

- **An approval naming no suggestions.** There would be nothing to show a later reader about what
  was merged and why.
- **An approval accepting a candidate below its own stated threshold.** A moderator who says "I am
  applying 0.9" cannot then approve something scoring 0.4. Either the threshold is wrong or the
  decision is, and silently accepting it would put a contradiction in the permanent record.

### Record and merge in one transaction

`review` persists the decision and applies the merge together. A recorded approval with no merge
applied would leave the audit trail asserting something that did not happen.

`mergedTargetSignalId` is null for anything other than an approval, so a caller cannot read "we
recorded your rejection" as "we merged them".

### Every route requires the moderation scope

All three routes require `MANAGE_MODERATION_QUEUE`. There is deliberately no route that merges
without a recorded decision behind it.

## Consequences

- A wrong merge is attributable and reversible in principle, and visible in `GET .../history`.
- What a reviewer saw is preserved even after the algorithm changes.
- The queue and the merge agree on what "similar" means.
- Rejections are as traceable as approvals.
- The suggestion queue survives a decline instead of resurfacing it weekly.

## Known limits

- **No UI.** The endpoints exist and are tested; a moderator-facing screen is not built. This
  closes the audit half of #36, not the workflow half.
- `SPLIT` is recorded but performs no un-merge. Reversing a merge is not implemented, so a split
  decision is currently a declaration without an effect. Stated rather than faked.
- Similarity is title-based. Two reports about the same water main with different wording score
  low and will never be proposed. Description similarity is not used.
- Category must match exactly (case-insensitively), so a report filed as `utilities` will not
  cluster with one filed as `water`.
- The decision table has no foreign key to the merged signals. Deleting a signal leaves the
  decision readable, which is the intended behaviour for an audit record, but it means a decision
  can outlive what it referred to.
- `similarityScore` returns `1.0` for the containment case even when the overlap is lower. That
  matches what `isSimilar` decided, deliberately: the two must not disagree.