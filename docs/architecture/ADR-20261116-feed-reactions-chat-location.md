# ADR-20261116: Feed reactions, community chat and live location

Status: accepted
Date: 2026-11-16

## Context

The feed could be read and commented, but a citizen could not react with a light touch, the
real-time community chat existed only behind the advanced "more tools" panel, and the report GPS
capture did not show a map a person could verify. Reactions and the chat already existed on the
backend (`/api/signals/{id}/react`, `/api/community/blog/{id}/react`,
`/api/community/rooms/**` with SSE), so the gap was the feed contract and the entry points.

## Decision

- Add `reactions` and `viewerReaction` to `CommunityFeedItemResponse`, read from the persisted
  reaction maps and the viewer's reaction in one query per content type. This is additive.
- Render **Like** and **Celebrate** on signal and blog cards with counts and `aria-pressed`;
  threads keep their existing message reactions and reply composer.
- Promote **Chat** to the core navigation and add a chat shortcut in the feed aside. The existing
  `CommunityRooms` view already streams messages over SSE; no new backend endpoint is needed.
- Keep thread titles as links to the specific thread, where nested replies and message reactions
  already live.
- Let the report form update the GPS capture and open the chosen point in OpenStreetMap. The
  coordinates stay optional and are still not stored by the map link.

## Consequences

- The feed DTO and OpenAPI schema gain two additive fields; no migration.
- One extra reaction query per feed content type, bounded to the page ids.
- Citizens can reach real-time chat in one click; the rooms page remains the single source for
  room history and presence.
- A fully public, anonymous social feed is still a separate privacy decision and is not enabled
  here.
