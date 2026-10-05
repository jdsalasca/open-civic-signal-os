# Round 51 evidence

Deliverable: a capped room history is visible as capped, and can be paged on request.

## The gap left by round 50

Round 50 bounded the API: `GET /api/community/rooms/{roomId}` takes a `limit`, defaults to 50, caps at
200, and answers with `hasMoreMessages` plus a true `messageCount`.

That made the payload honest. It did not make the screen honest. The view rendered whatever array
arrived, so a coordinator opening a room with 1,204 messages saw the 50 newest and had no way to know
1,154 were missing. The UI said "this room"; the API said "this page". Nothing errored.

This is the same failure mode as the rounding bug in the score history: a silent truncation that the
consumer cannot detect. `hasMoreMessages` was built so a consumer *could* detect it. It was not used.

## What the reader is now told

```
Showing the most recent 50 of 1204 messages.        [ Load older messages ]
```

Both numbers, because either alone misleads: "50" hides that there are 1,204, and "1,204" next to 50
rows looks like a rendering bug. The count shown is `room.messages.length` (what was actually
rendered) against `room.messageCount` (what actually exists) - not the requested `limit`.

The control is `CivicButton variant="ghost"`. My first pass used a `u-btn-quiet` class that exists in
no stylesheet; the repo rule is SCSS or an existing primitive, never a class invented at the call
site.

When the room fits entirely (`hasMoreMessages: false`) there is no note and no button. Offering to
load more of a room that fits would train people to distrust the control.

Paging is by raising the request limit (`50 -> 100 -> 150 -> 200`), not by an offset or cursor. The
API contract is limit-only; inventing an offset parameter here would have meant a second pagination
scheme on one endpoint. The ceiling is `ROOM_PAGE_MAX = 200`, matching the server cap - past it the
button disappears rather than asking for a value the server will clamp and silently under-deliver.

## The root cause of the 6 red tests

Worth recording because the first diagnosis was wrong.

The six failures were **not** a rendering bug. `net::ERR_CONNECTION_REFUSED at
http://localhost:3002/...` - the capture server listens on 5199 and `playwright.config.ts` defaults
`baseURL` to 3002, so the run needed `BASE_URL=http://127.0.0.1:5199`. The blank screenshot was a
browser that never loaded a page. I had already started rewriting the role fixture on a hunch that an
invalid `activeRole` blanked the route guard; the role was genuinely wrong (`COMMUNITY_COORDINATOR` is
not a `UserRole`, the set is `SUPER_ADMIN | PUBLIC_SERVANT | CITIZEN | GUEST`) and that fix stayed, but
it was not what the red was. A blank page and a wrong fixture produce the same symptom, and only one
of them was the cause.

Two fixture corrections survived that investigation, both real:

- the room list selects its own first room, so seeding a `room-storage` key did nothing - the active
  room comes from `loadWorkspace()`;
- the workspace endpoint returns `CommunityRoomSummary`, not `CommunityRoomDetail`. The stub was
  returning detail objects with a `messages` array the summary type does not have.

`detail()` also lost its `as CommunityRoomDetail` cast. The cast was silencing exactly the drift it
should have caught.

## Tests fail when the UI lies

`src/tests/community-rooms-history.spec.ts` - 3 cases, chromium + mobile-chrome, 6 total.

| Mutation | Result |
| --- | --- |
| Button re-requests the same page: `setMessageLimit(ROOM_PAGE_SIZE)` | 1 failed - the paging test |
| Button always rendered: `{true && (` | 1 failed - the "whole room loaded" test |
| Unmutated | 6 passed |

The paging test asserts on `requestedLimit`, the value the stub actually read off the outgoing URL. A
button that re-fetches the same page is a control that lies, and asserting "the button exists" would
have passed against it.

## Visual review

`truncated-desktop-history-note.png`, `truncated-mobile-history-note.png` - the note and control in
place. The note sits at the top of a message list inside an inner scroller, so neither a `fullPage`
screenshot nor a scrolled-to-bottom one frames it; the capture scrolls the control itself into view.

`truncated-desktop.png` / `-lower.png` - workspace, room list, and the message list at rest and at the
bottom. `complete-desktop.png` / `-lower.png` - a room that fits: no note, no button.

Mobile stacks the note and the control without overlapping the bottom nav.

All three captures logged `clean`, and `scripts/capture-community-rooms.mjs` now prints the URL of
any 4xx/5xx. It found one real omission: the shared layout calls `/api/auth/me` on every screen, which
proxied to a backend that was not running and produced two 500s. Console text said "500"; only the URL
says which call. Worth the three lines.

## Known limits

- **The database still loads every message.** `getRoom` calls
  `findByRoomIdOrderByCreatedAtDesc` and applies `.limit(effectiveLimit)` in memory. The payload is
  bounded; the query is not. A room with a very large history still reads it all to count and slice it.
  A pageable query plus a separate count is the fix, and it belongs in its own round because it changes
  the repository signature.
- **Paging is not incremental append.** Raising the limit re-fetches the whole page. Fine at these
  sizes; the API would need a cursor before it is not.
- `1204` is not thousand-separated. Consistent with the stat card above it, which also renders the raw
  count. Left alone rather than churn the test that pins it.