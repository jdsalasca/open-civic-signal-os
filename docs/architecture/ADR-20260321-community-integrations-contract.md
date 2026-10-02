# ADR-20260321: Outbound Integrations And Delivery Audit Contract

- Status: Accepted
- Date: 2026-03-21
- Story: `OCS-P1-053`

## Context

Community operations already depend on external surfaces: calendars for activities, message channels for official updates, email for digests. Today the platform has no way to reach any of them, so every coordinator re-publishes the same civic fact by hand into two or three places. When that manual step fails, nothing records that it failed.

The trust problem is specific: an outbound delivery that silently fails is worse than no integration, because the community believes the message went out. The failure has to be visible and retryable, and the delivery has to leave a record.

## Decision

Introduce a backend-owned outbound integration contract with:

- one integration per channel endpoint, storing `targetUri` and only the **hash** of the signing secret
- four declared channels: `WEBHOOK`, `EMAIL_DIGEST`, `CALENDAR_FEED`, `MAP_LINK`
- three event types: `OFFICIAL_ANNOUNCEMENT`, `ACTIVITY_SCHEDULED`, `RESOURCE_BOOKED`
- one delivery record per attempt, carrying `status`, `attempts`, `lastError`, and `completedAt`
- per-integration `consecutiveFailures`, `lastAttemptAt`, and `lastSuccessAt`
- one permission scope: `MANAGE_INTEGRATIONS`

Delivery uses a single HTTP connector built on the JDK's own `java.net.http.HttpClient`. No new dependency is introduced. `WEBHOOK` and `CALENDAR_FEED` share that transport and differ only in payload and content type, because a calendar feed is an HTTP endpoint serving `text/calendar`; the connector builds an RFC 5545 `VCALENDAR` body for that channel.

Two honesty decisions are load-bearing:

- **Channels without a connector fail loudly.** `EMAIL_DIGEST` and `MAP_LINK` can be configured, but publishing to them records a `FAILED` delivery with `No connector is registered for this channel yet.` rather than reporting a fake success. `dispatchable: false` on the integration says the same thing in the UI before anyone configures one.
- **Fan-out records every target, including failures.** The publish response returns `targeted`, `delivered`, `failed`, and `skippedNoConnector` counts plus one delivery record per target, so a partial fan-out is legible rather than a single boolean.

Calendar feeds only receive scheduled events; webhooks receive everything. Retries are explicit: a `FAILED` delivery on an integration with `autoRetry` can be re-delivered through an endpoint that returns `409` otherwise, and only `FAILED` deliveries are retryable.

## Consequences

Positive:

- delivery failure is a first-class, visible, retryable record instead of a silent drop
- no integration framework or HTTP client dependency was added
- channels are honest about what is actually wired, so nobody configures an email digest expecting delivery
- the secret is stored hashed and never returned by any endpoint

Trade-offs:

- delivery is synchronous inside the request with a 5s timeout. A slow endpoint adds latency to publish, and a large fan-out multiplies that. Move to a queue plus worker before integration counts or endpoint latencies grow.
- the HMAC helper exists but signatures are not yet attached to outgoing headers, so receivers cannot yet verify the sender end to end. The hash is stored so the receiver secret cannot leak, but verification is not closed.
- `MAP_LINK` and `EMAIL_DIGEST` are declared but not dispatched. Declaring a channel costs nothing and keeps the contract stable, but it does mean the enum advertises capability the system does not have yet.
- there is no outbound retry scheduler. Retries are operator-initiated from the admin surface.

## Validation

- `CommunityIntegrationIT` stands up a real local `com.sun.net.httpserver.HttpServer` and asserts end-to-end delivery, not a mock: announcement fan-out with `application/json` body, failure recording with `Endpoint returned HTTP 500`, retry promoting a failed delivery to `DELIVERED` and resetting `consecutiveFailures`, calendar feed restricted to scheduled events and emitting `text/calendar` with a `VCALENDAR` body, a connector-less channel failing without touching the endpoint, disabled integrations receiving nothing, member denial, and invalid target/secret rejection
- OpenAPI updated in the same change set with integration, delivery, center, and fan-out schemas including the 400/403/409 branches
- `community-integrations.spec.ts` covers failure visibility, retry from the UI, and connecting a channel without leaking the secret