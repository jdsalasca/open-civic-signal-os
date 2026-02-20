# ADR-20260220-community-collaboration-model

- Status: accepted
- Date: 2026-02-20
- Owners: platform team

## Context

Open Civic Signal OS needs community-scoped collaboration: multi-membership, local roles, inter-community threads, public-servant progress blogs, and a unified feed.
The existing model had only global roles and no community context in APIs.

## Decision

1. Add explicit community domain model:
- `communities`
- `community_memberships`
- `community_membership_audit`
- `community_threads`
- `community_thread_messages`
- `community_blog_posts`

2. Add community-scoped RBAC roles:
- `MEMBER`
- `MODERATOR`
- `COORDINATOR`
- `PUBLIC_SERVANT_LIAISON`

3. Keep ranking ownership in backend and scope signal APIs by active context:
- support `X-Community-Id` header on signal reads/writes
- enforce membership validation before scoped access

4. Expose collaboration endpoints under `/api/community/*` and membership endpoints under `/api/communities/*`.

5. Provide unified feed endpoint typed as:
- `signal`
- `blog`
- `thread-update`

## Consequences

### Positive

- Deterministic and auditable role enforcement by community.
- Explicit support for cross-community coordination.
- API consumers can scope workflows by selected community context.

### Tradeoffs

- Additional schema complexity and migration overhead.
- More authorization checks in controller/service paths.

## Migration Notes

- Adds Flyway migration `V5__Community_Expansion.sql`.
- Adds optional `signals.community_id` for scoped signal lifecycle.
- Existing clients continue working without `X-Community-Id`; scoped behavior activates when header is provided.
