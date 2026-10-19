# ADR-20260321: Community Trust Pulse

- **Status:** Accepted
- **Date:** 2026-03-21
- **Decides:** GitHub #53
- **Contract:** `packages/contracts/openapi.yaml` (`/api/community/trust-pulse`)

## Context

The platform already had computed trust metrics: closure rate, participation coverage, execution
completion, median resolution. Those measure **what the platform did**.

The issue asks for community trust pulse inputs. That is a different thing: what residents **think**
of the platform. The distinction is the whole design, because rolling the two into one "trust score"
would let the platform grade its own homework.

## Decision

### Perception and performance stay separate

`CommunityTrustPulseService` is a separate service, a separate table, and a separate endpoint from
`CommunityTrustMetricsService`. Nothing merges them.

The computed cards answer "did the institution keep up". The pulse answers "do residents believe it
did". A platform that reports a single blended number has hidden which of those it is measuring, and
the two can diverge sharply: a community can have a 90% closure rate and a 2/5 trust score, and that
gap is the most interesting fact available.

### No average below a minimum sample

`MIN_RESPONSES_FOR_AVERAGE = 5`. Below it, `average` is **null** and `note` says why.

Null rather than a caveated number, deliberately. A mean of 4.3 over three answers is noise, and a
number that appears in a response will be quoted; a caveat next to it will not. The floor is
published at `/minimum-sample` so a client can explain it *before* a resident answers rather than
after.

Pinned by `noAverageShouldBeReportedBelowTheMinimumSample`, which asserts the field is absent rather
than present-and-low.

### One answer per person per month, enforced by the database

A unique index on `(community_id, respondent_id, period_key)`. Answering again **replaces** the
previous answer and reports `replacedPrevious: true`.

A pulse that counts the loudest respondent twice is not a measure of the community. And a resident
changing their mind should update their answer, not stack a second one.

### Comments are never published verbatim

The comment column exists so a person can read it. It is not returned by any endpoint.

A resident writing "the man at number 12 never fixes anything" did not consent to that becoming a
public dataset row. The aggregate's `interpretation` says so on every response, so a reader knows
the free text is not being silently discarded either.

### The interpretation says what a low score might mean

Every aggregate carries it: this is a **self-selected** sample, so a low score may mean
dissatisfaction **or** may mean the residents who are content did not reply. Both readings are
consistent with the same number, and a reader who is not told that will pick the one that suits
them.

### Membership required to answer and to read

Someone outside a community has no standing to answer for it. Reading is also membership-gated: a
community's opinion of its platform is not privileged information, but it is not the whole world's
either.

### The period is a closed month

`YYYY-MM`, defaulting to the previous completed month. On the 3rd of the month the current one is
three days of answers, and calling that "this month's pulse" would misdescribe the community. Same
reasoning as the digest and the transparency report.

## Consequences

- Residents can say what they think, and the platform cannot blend that with its own performance.
- A number that would be quoted is either real or absent, never caveated.
- One person cannot weight the pulse by answering repeatedly.
- Free text is collected for humans and stays out of the data.
- The self-selection caveat travels with every number.

## Known limits

- **No UI.** The endpoints exist and are tested; no screen asks a resident to answer. That is the
  remaining half of this issue.
- **No prompt or reminder.** Nothing tells a resident the pulse exists, so participation depends on
  them finding it.
- **No anonymity.** Responses are tied to `respondent_id` by the unique index. A resident cannot
  answer anonymously, which is a real barrier to honest answers about an institution they may
  depend on. Anonymous-but-once is a hard problem and is not attempted here.
- **No weighting by anything.** Every response counts once. A community where 5 of 400 members
  answer gets the same treatment as one where 5 of 6 do, apart from the sample size being visible.
- **No trend analysis.** `history` lists months; nothing computes whether trust is rising or
  falling, and with small samples a trend line would be misleading anyway.
- **No moderation of comments.** They are stored and not published, so there is nothing to moderate
  yet. A screen that shows them to a coordinator would need one.
- The minimum sample is a constant, not per-community. A small community may never reach it, which
  is honest but means the pulse reports no average indefinitely.