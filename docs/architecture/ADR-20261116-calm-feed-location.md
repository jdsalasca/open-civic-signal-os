# ADR-20261116: Calmer dark palette, full-width feed, and a location-aware home

Status: accepted
Date: 2026-11-16

## Context

Field feedback from the production build was blunt: the dark theme felt overloaded, the fixed right
column took space without adding much, the feed only showed one conversation and offered no visible
reactions, and the app showed **Bogotá D.C.** while the reader was in **Tunja**, with no sign of
their barrio or a way to change it.

## Decision

- **Palette:** the dark theme keeps aqua for icons and accents but softens it to `#4fd1c5`. Solid
  primary controls use a new `--brand-fill` (deep teal `#0f766e` with white text) instead of an aqua
  slab, active filters use a translucent aqua surface, and the active nav item is a flat neutral
  surface instead of a mint/blue gradient. The sidebar brand is one compact row and the duplicated
  community card is gone; the interface-mode badge lives in the profile footer.
- **Feed layout:** the right column is removed. The feed is a centered column (max 56rem) with the
  alerts toggle in the toolbar and **Tendencias** as a full-width card after the timeline. Chat stays
  in the main navigation.
- **Thread cards are first-class:** the feed DTO carries `rootMessageId`, the first message's
  reactions and viewer state, and `commentCount` as replies (total − 1). The card shows **Me gusta /
  Celebrar / Comentar** for conversations too, mapping LIKE/CELEBRATE to the existing emoji reaction
  keys (`👍`/`🙌`) used by thread messages.
- **Location:** the home asks for geolocation once per tab and reverse-geocodes it. It shows the
  detected **barrio · localidad · municipio** under the title. When the active community's name and
  breadcrumb do not contain the detected city, a dismissible banner says where the reader is and
  offers **Cambiar comunidad**, which opens onboarding. Joining or creating a community there now
  selects it as active before returning.

## Consequences

- The feed uses the full width without hiding community tools; trends are visible without a side
  column.
- Conversations expose the same participation vocabulary as reports and announcements.
- The app no longer silently keeps a distant community: it states the mismatch and offers a path.
- Geolocation is requested once per tab; denial leaves the feed unchanged. The banner is dismissed
  per tab.
- No schema or dependency change; the feed response gains one nullable field.

## Files

- `apps/web-react/src/styles/theme.scss`
- `apps/web-react/src/styles/main.scss`
- `apps/web-react/src/components/Layout.tsx`
- `apps/web-react/src/views/CommunityFeed.tsx`
- `apps/web-react/src/views/Onboarding.tsx`
- `apps/api-java/.../service/CommunityCollaborationService.java`
- `apps/api-java/.../repository/CommunityThreadMessageRepository.java`
- `apps/api-java/.../web/dto/CommunityFeedItemResponse.java`
- `packages/contracts/openapi.yaml`
