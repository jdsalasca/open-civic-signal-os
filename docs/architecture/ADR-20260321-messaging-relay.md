# ADR-20260321: WhatsApp And Telegram Relay

- **Status:** Accepted
- **Date:** 2026-03-21
- **Decides:** GitHub #7
- **Contract:** `packages/contracts/openapi.yaml` (`CommunityIntegrationChannel` gains `WHATSAPP` and `TELEGRAM`)

## Context

The digest could reach email. A community that coordinates on WhatsApp could not receive it, which
is most of the communities this platform is for.

The connector interface from the digest delivery work already existed to plug into. What was missing
was the channels and the transport.

## Decision

### The bot belongs to the community, not the platform

The bot token is the integration's `secretHash` field, and the chat or group id is `targetUri`.

A platform-owned bot would make every message come from the platform, and **a resident replying to it
would reach nobody who can act**. The community's own bot means the reply goes to the community.

A missing token is a clear refusal naming that reason, not a silent no-op. Pinned by
`aMissingBotTokenShouldBeRefusedWithTheReason`.

### `secretHash` holds a token for these channels, and that is a lie in the field name

For the webhook channel, `secretHash` is a hash of a signing secret that never leaves the platform.
For messaging, the token has to be **sent to the provider**, so it cannot be hashed.

The field name is therefore wrong for these two channels. Renaming it would touch the webhook path
and the migration, so it is documented here instead: a reader who assumes `secretHash` is always a
hash will be surprised.

### Each provider gets its own request shape

| Provider | Endpoint | Token placement |
| --- | --- | --- |
| Telegram | `POST /bot<token>/sendMessage` | In the path, which is how the API works |
| WhatsApp | `POST /<phoneNumberId>/messages` | `Authorization: Bearer`, which is better |

Telegram's token in the path means it appears in any request log along the way. That is a property of
the provider rather than of this code, and it is stated rather than glossed.

### A provider error carries the provider's reason

The response body is the only place the reason exists, so it is included in the failure. Pinned by
`aProviderErrorShouldCarryTheProvidersReason`.

### A long message is truncated, not rejected

With a marker, so a reader knows the message was cut rather than ending oddly. A digest that exceeds
the limit should still reach the group.

### An empty message is refused

A blank message reaching a group is worse than a failed one.

### The provider base URL is configurable

`app.messaging.telegram-api` and `app.messaging.whatsapp-api` default to the real endpoints and can be
pointed at a stub. Without this the only way to exercise the connector is to call the real providers,
which a test must never do. This was added because the first version of the test failed for exactly
that reason.

### `acceptsEvent` gives messaging the digest and announcements

Not scheduled events: a calendar entry in a chat thread is noise. Pinned by the existing
`acceptsEvent` switch, which now has a case for both channels.

## Consequences

- A community that coordinates on WhatsApp or Telegram can receive the digest.
- Replies reach the community, not the platform.
- A missing token fails with a reason a community can act on.
- The connector is testable without touching the real providers.
- A provider outage is recorded with the provider's own message.

## Known limits

- **No inbound.** This is send-only. A resident replying to the bot does not reach the platform, so a
  report filed in a WhatsApp thread still has to be entered by hand or imported.
- **No media.** Text only. A photo attached to a report is not relayed.
- **No message threading or replies.** Each digest is a standalone message.
- **No token rotation.** Changing a bot token means editing the integration. The stored value is sealed
  under `v1`, so rotating `INTEGRATION_CREDENTIAL_KEY` needs a re-encryption pass that does not exist
  yet.
- **No delivery receipt.** The provider accepting the message is not the same as a resident reading
  it, and the platform cannot tell the difference.
- **No rate-limit handling.** A provider returning 429 is recorded as a failure and retried by the
  existing retry endpoint, which may hit the same limit.
- **No per-platform formatting.** The same body goes to both, so neither gets its native formatting.

## Superseded in part

This ADR originally recorded two defects as accepted limitations, on the reasoning that naming a
hazard is better than hiding it. Both have since been fixed, because a documented hazard is still a
hazard:

- **`secretHash` held the token in the clear.** Now sealed in `credential_ciphertext` with AES-GCM.
  See `ADR-20260321-integration-credential-storage.md`.
- **`targetUri` was validated as an http(s) URL for every channel**, so a Telegram chat id could not
  pass validation and the integration could not be created. Now validated per channel, and WhatsApp
  carries its phone-number id separately from the recipient.