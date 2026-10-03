# ADR-20260321: Digest Delivery Through The Integration Layer

- **Status:** Accepted
- **Date:** 2026-03-21
- **Decides:** the delivery half of GitHub #20, shared with #22 and #7
- **Contract:** `packages/contracts/openapi.yaml` (`WEEKLY_DIGEST` event type, `deliveredToChannels`)

## Context

The digest was generated and sealed but reached nobody. `EMAIL_DIGEST` existed as a channel with no
connector, and `CommunityIntegrationService` injected one concrete connector, so a second channel
had nowhere to plug in.

The blocker I recorded earlier was that delivery could not be **verified**: there is no SMTP in the
test profile, so any claim about sending would have been an assumption. That is now addressed, and
the way it was addressed is the first decision below.

## Decision

### The mail transport is mocked, and the boundary is stated

`WeeklyDigestDeliveryIT` uses `@MockBean JavaMailSender`. That verifies what this codebase owns:
the connector resolves the configured recipient, composes the message, and hands it to the mail
transport. It does **not** re-verify SMTP, which is Spring's and the mail server's job.

The test class says so in its own javadoc. Claiming more than this would be claiming to have tested
something we did not, which is the failure mode the triage protocol exists to prevent.

What it does verify end to end is the property that matters operationally: **publishing a week sends
once, and publishing the same week again is refused before anything is sent.**

### Connectors are selected, not injected

`CommunityIntegrationConnector` is an interface with `supports(channel)` and `deliver(...)`.
`CommunityIntegrationService` holds a `List<CommunityIntegrationConnector>` and resolves one per
channel.

A channel nobody serves returns `NO_CONNECTOR` rather than silently doing nothing, which is this
layer's existing documented behaviour: an integration a community configured but the platform cannot
serve must look broken, not look idle.

`MAP_LINK` is deliberately **not** filtered out of event resolution, even though it has no
connector. Filtering it would make a configured-but-unservable integration look idle, which is the
opposite of failing visibly. This was caught by an existing test whose premise changed when
`EMAIL_DIGEST` gained a connector.

### `acceptsEvent` is stated per channel, not defaulted to yes

The old rule was "everything except calendar". Adding the email channel under that rule would have
sent **every official announcement to residents as a weekly bulletin**.

It is now a switch with a case per channel:

| Channel | Receives |
| --- | --- |
| `CALENDAR_FEED` | scheduled events only |
| `EMAIL_DIGEST` | the weekly digest only |
| `WEBHOOK` | everything |
| `MAP_LINK` | everything, so it fails visibly with `NO_CONNECTOR` |

Pinned by `anEmailIntegrationShouldNotReceiveOrdinaryAnnouncements`.

### Delivery happens inside the publish transaction

`publishDigest` fans out after recording the publication, in the same transaction. A week that says
published has actually been handed to whatever channels the community configured.

Idempotency comes from the existing unique index on `(community_id, week_key)`: a second publish is
refused before it reaches the fan-out, so a retry cannot deliver twice. Pinned by
`publishingTheSameWeekTwiceShouldSendOnlyOnce`, which asserts the mail transport was called exactly
once.

### A misconfigured target fails the delivery, not the publish

A community can set `targetUri` to anything. A URL where an address belongs produces a recorded
failed delivery with a clear reason, and the publish still succeeds — the digest is published, one
channel did not accept it, and the existing retry endpoint can re-send without republishing.

Pinned by `aMisconfiguredTargetShouldFailClearlyRatherThanBlowUpThePublish`.

### An empty body is refused

The connector refuses to send a blank digest. A blank bulletin reaching residents is worse than a
failed one, and the digest service should never produce it, so a bug upstream surfaces as a failed
delivery rather than as an empty email someone has to explain.

## Consequences

- A published week reaches the channels a community configured.
- A week cannot be delivered twice.
- A channel failure is recorded, attributable, and retryable without republishing.
- An email integration cannot receive announcements, and a calendar feed cannot receive a digest.
- A channel with no connector still fails visibly.

## Known limits

- **SMTP itself is not exercised.** The mock verifies composition and handoff, not deliverability.
  A real Mailpit-backed test would close this and is the natural next step; Mailpit is already
  pinned in the runtime compose.
- **No scheduling.** Publishing is manual. A weekly job is #22's remaining work.
- **No unsubscribe or preference handling.** A resident on a community's mailing list cannot opt out
  through the platform.
- **No per-recipient rendering.** One body per community, sent to one configured address. A
  community wanting per-member delivery needs a recipient model that does not exist.
- **`targetUri` is community-supplied**, so the platform can send mail to addresses a community
  chooses. That requires `MANAGE_INTEGRATIONS`, and the webhook channel already had the same shape,
  but it is worth stating explicitly.
- **`secret_hash` is `NOT NULL` for every channel**, including ones that sign nothing. The test sets
  a placeholder for email. A nullable column or a channel-aware constraint would be cleaner.
- Delivery is synchronous inside the publish request. Fine at community scale; a queue is the
  upgrade path the connector's own comment already names.