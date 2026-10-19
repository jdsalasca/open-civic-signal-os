# ADR-20260305: Community Thread Relevance Contract

- Date: 2026-03-05
- Status: Accepted

## Context

`story:OCS-P1-034` requires community discussions to remain usable as volume grows. The existing `/api/community/threads` contract exposed paged threads ordered implicitly by recent activity, but it did not tell clients:

- how to request deliberation ordering explicitly
- why one thread should appear above another
- how deep nested replies are allowed to go

That left thread ordering under-specified and made UI hierarchy depend too heavily on local assumptions.

## Decision

Extend the community thread contract so backend remains the owner of deliberation ordering and reply-structure rules.

Changes:

1. `GET /api/community/threads` accepts `sortBy=RELEVANCE|RECENT`
2. `CommunityThreadResponse` now exposes:
   - `totalMessages`
   - `totalReplies`
   - `totalReactions`
   - `relevanceScore`
   - `relevanceSummary`
3. `CommunityThreadMessageResponse` now exposes:
   - `depth`
   - `directReplyCount`
4. Backend enforces a maximum nested reply depth of `4`

## Consequences

Positive:

- frontend can render discussion hierarchy without inventing sorting logic
- communities can understand why one thread is surfaced ahead of another
- nested discussions stay bounded and readable on mobile and desktop

Tradeoffs:

- relevance ordering is computed in service memory for now, not persisted
- future large-scale thread volumes may require query-side or materialized relevance support

## Validation

- backend integration coverage for `RECENT` vs `RELEVANCE`
- backend validation for reply-depth limit rejection
- frontend route/state coverage for persisted thread sorting and filtering
