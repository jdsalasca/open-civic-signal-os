# ADR-20260321: Weekly Digest Scheduling

- **Status:** Accepted
- **Date:** 2026-03-21
- **Decides:** GitHub #22, and the scheduling half of #6 and #7
- **Contract:** `packages/contracts/openapi.yaml` (`/api/community/weekly-digest/schedule-history`)

## Context

Digest delivery shipped earlier: `EMAIL_DIGEST` has a connector, publishing fans out, and a week can
only be published once. What was missing was the schedule, and I recorded the blocker at the time:

> who is accountable when a scheduled digest goes out wrong?

That is not a cron question. A scheduler that sent bulletins on its own would mean a wrong digest
reaches residents with nobody accountable for it.

## Decision

### The scheduler prepares; it does not publish

This is the answer to the blocker, not a deferral.

`DigestSchedulerService` generates the digest for the previous completed week, records that it is
ready in `digest_schedule_runs`, and stops. Publishing remains a deliberate act by a person who can
be asked why.

There is deliberately **no `PUBLISHED` outcome** in the enum. `PREPARED`, `SKIPPED`, `FAILED` are the
only three, and the interpretation says on every report that "nothing has reached residents".

Pinned by `aRunShouldPrepareADigestAndPublishNothing`, which asserts the publication table is still
empty after a run.

### The prepared digest is kept, not recomposed

"Prepared" has to mean the artifact exists. The first implementation composed a digest, read its item
count for the run detail, and discarded the rest; publishing then recomposed from live data.

The gap was small and real. `priorityScore` is mutable, and rescoring exists as a feature, so a
coordinator could review a digest on Monday and residents could receive a different one on Wednesday:
the score moved, the order moved, the body moved, and the content hash a resident got corresponded to
nothing anybody had looked at. The digest's reason for carrying a hash is that a recipient can verify
what they received, and a hash of an artifact nobody reviewed verifies nothing.

So `community_digest_preparations` stores the body, the hash, the counts and the rendered items, and
**the prepared artifact wins everywhere**: the preview a coordinator reads and the body that gets
delivered come from the same row. Recomposition happens only for a week with no preparation, which is
the case where a coordinator runs the digest by hand with no scheduler involved.

The counts are columns rather than recomputed values because they appear in the rendered body, and the
body is what residents receive. Anything not stored cannot be reproduced from a review.

One consequence worth stating: when a preparation exists, the `limit` argument at publish time no
longer changes anything. Passing a different limit would produce a digest different from the one
reviewed, which is the defect, so the argument is ignored rather than honoured.

Pinned by `publishingShouldSendTheArtifactThatWasPreparedAfterTheWorldChanges`, which also asserts the
counterfactual — that recomposing after the change really does differ — so the test cannot pass
vacuously.

### A second preparation keeps what is under review

`prepare` returns empty when a preparation exists for the week, so a repeated fire cannot overwrite
an artifact somebody may already have looked at. Combined with the run-level skip, a week is prepared
exactly once.

### Monday morning, for the week that just ended

`0 0 6 * * MON`. Monday rather than Sunday night because a digest generated at 23:59 on Sunday would
race the last reports of the week, and a report filed at 23:58 would land in the wrong bulletin.

### Idempotent per community per week, enforced by a unique index

A scheduler that fires twice, or a restart mid-week, must not produce two records for the same week
and make it look like two attempts happened. Pinned by
`aSecondRunInTheSameWeekShouldSkipRatherThanDuplicate`.

### Disabled by default

`app.digest.scheduler.enabled` defaults to `false`. A job that started generating digests the moment
it was deployed would surprise a community that has not decided to run a weekly bulletin yet.

### A failure is recorded, not swallowed

A run that cannot generate a digest records `FAILED` with the reason. The interpretation says a
failure means a community gets no bulletin that week unless someone looks, because a scheduler that
throws into the void leaves no trace of why nothing happened.

### The scheduler skips the membership check, and that is not a bypass

`buildDigestForScheduler` composes without a membership check, because the scheduler has no user to
check. It is not a hole in the permission model: the scheduler runs inside the platform, its runs are
recorded in `digest_schedule_runs`, and every user-facing route still goes through `buildDigest` and
its check.

### `@EnableScheduling` was missing, and that was a real bug

`RateLimitService.cleanup()` carried `@Scheduled(fixedRate = 60000)` with no `@EnableScheduling`
anywhere in the application. **The method never ran**, so the rate-limit map grew without bound — a
slow memory leak that nothing would have surfaced until a long-running instance ran out of heap.

Found while wiring this scheduler. Fixed in the same change, because a scheduler that does not run is
worse than no scheduler: it looks like a working cleanup.

## Consequences

- A community gets a digest prepared every week without anyone remembering.
- Nothing reaches residents without a person deciding to publish.
- A restart or a double fire cannot produce two runs for one week.
- A failed run is visible rather than silent.
- The rate-limit cleanup now actually runs.

## Known limits

- **No reminder.** The scheduler prepares and records it; nothing tells anyone a digest is waiting.
  A community could still discover on Friday that no bulletin went out. The history endpoint is the
  mitigation, and it requires someone to look.
- **No per-community schedule.** One cron for every community. A community wanting a different day
  would need a per-community setting.
- **No timezone handling.** The cron runs in the server's zone, so a community in another zone gets
  its digest at an odd hour. Correct for a single-instance deployment and wrong for a federated one.
- **No retry.** A failed run is recorded and not retried until the next week. A transient failure
  costs a week.
- **No dry-run mode.** An operator can call `prepareForAllCommunities` directly, but there is no
  "show me what would be prepared" without recording a run.
- **The scheduler is a single instance concern.** Two instances running the same cron would both
  attempt the run; the unique index makes the second a `SKIPPED`, which is correct but means the
  second instance does wasted work.
- **No alerting on failure.** Same gap as the freshness monitor: it reports, nothing pages.