# ADR-20261117: Votes and five-emoji reactions on feed cards

Status: accepted
Date: 2026-11-17

## Context

The feed offered one heart and no negative signal. The user asked for up/down votes, the five
Facebook-style emoji reactions, reactions and votes on conversations, and a comment box that uses
the card width instead of a narrow strip.

## Decision

- **Votes:** each card exposes **UPVOTE** and **DOWNVOTE** counts. They reuse the existing reaction
  endpoint and `UserReaction` storage, so a user can hold only one reaction per item; choosing a
  different one moves the vote. No schema or contract change.
- **Five reactions:** the picker offers 👍, ❤️, 😂, 😮 and 😢. For signals and posts the 👍 option
  uses the existing `LIKE` key so current likes remain valid; for thread entries it uses `👍`, which
  is the key the thread screen already uses. The other four are emoji keys.
- **One shared bar:** `FeedReactionBar` renders votes, the picker trigger and the menu for every feed
  card. The menu closes on outside click and Escape, uses `role="menu"` / `menuitemradio` and shows
  per-option counts.
- **Thread screen:** each message gains the same UPVOTE/DOWNVOTE buttons next to its emoji bar, so
  votes placed on a conversation in the feed are visible and actionable there.
- **Comments use the card:** the reply block and textarea take the full card width, start at three
  rows and a comfortable line height.
- **Trending:** vote keys are excluded from the reaction total used by **Tendencias**; reactions
  remain the signal.

## Consequences

- Citizens can express agreement, disagreement and five kinds of reaction from one compact bar.
- Existing likes stay counted; legacy `CELEBRATE` counts remain in the stored map but are not one of
  the five visible options.
- No API, contract or schema change; the reaction map was already open-ended.
- A single reaction per user per item is a deliberate constraint, inherited from the existing
  reaction service.

## Files

- `apps/web-react/src/components/feed/FeedReactionBar.tsx`
- `apps/web-react/src/views/CommunityFeed.tsx`
- `apps/web-react/src/views/CommunityThreads.tsx`
- `apps/web-react/src/styles/main.scss`
- `apps/web-react/src/i18n.ts`
- `apps/web-react/src/tests/community-feed.spec.ts`
