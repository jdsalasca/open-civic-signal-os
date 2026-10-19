# ADR-20260321: Integration Credential Storage

- Status: accepted
- Date: 2026-03-21
- Relates to: `ADR-20260321-messaging-relay.md`, `ADR-20260321-runtime-secret-policy.md`

## Context

The messaging relay shipped unable to authenticate. Three defects, all in the same path:

1. `CommunityIntegrationService.createIntegration` hashed every channel's secret with
   `WebhookCommunityIntegrationConnector.hashSecret`. The relay then read that column as the bot
   token, so what went to Telegram was a SHA-256 digest and the provider answered **401**.
2. `targetUri` was validated as an absolute http(s) URL for all channels. A Telegram chat id is not a
   URL, so **the integration could not be created at all**.
3. WhatsApp used one identifier for two different provider concepts: the Cloud API path is the
   business **phone-number id**, while the form's `to` is the **recipient**. Using the recipient for
   both would 404 on every send.

Defect 1 survived nine connector tests because they constructed `CommunityIntegration` directly with
a plaintext token — a value the service never produced. **A test that hand-assembles the entity cannot
see a bug in how the entity is built.**

The first version of this work also put the token in the clear in a column named `secret_hash`, and the
relay ADR documented that as a known limitation. Documenting a hazard is not the same as not shipping it.

## Decision

### Two columns, because these are not the same kind of secret

| Column | Holds | Protection | Leaves the platform |
| --- | --- | --- | --- |
| `secret_hash` | Webhook signing secret | SHA-256 | Never |
| `credential_ciphertext` | Bot token | AES-256-GCM | Yes, to the provider |

`secret_hash` becomes nullable: messaging rows do not use it.

### AES-256-GCM from the JDK, keyed by configuration

- Key from `INTEGRATION_CREDENTIAL_KEY`. A 64-character hex string is used directly; anything else is
  SHA-256 derived, so a short passphrase yields a full-length key instead of a startup failure on a
  length rule nobody would remember.
- Format `v1:<base64 iv>:<base64 ciphertext>`. The version prefix is what makes key rotation possible
  without guessing: an old row still says `v1`.
- **A fresh IV per row**, so two communities using the same token are not visibly identical.
- **Authenticated**, so a modified row fails to decrypt rather than decrypting to garbage. Without
  that, a corrupted token looks like a provider outage and sends someone hunting the wrong problem.

### No default key, and a refusal rather than a fallback

A missing key does not silently store tokens in the clear. `createIntegration` fails with a message
naming the variable. Failing at configuration is a clear error for whoever is setting the integration
up; failing at delivery means the token is already stored wrongly and the community finds out when the
bulletin does not arrive.

### Channel-aware validation

`targetUri` means four different things across the enum, so it is validated per channel:

| Channel | `targetUri` | `providerResourceId` | `secret` |
| --- | --- | --- | --- |
| `WEBHOOK` | absolute http(s) URL | rejected | signing secret, hashed |
| `EMAIL_DIGEST` | email address | rejected | unused, transport is the platform's |
| `TELEGRAM` | numeric chat id or `@handle` | rejected | bot token, encrypted |
| `WHATSAPP` | recipient number | **required** | bot token, encrypted |

Telegram chat ids may be **negative** (`-100…` for supergroups), so a digits-only rule would reject
exactly the large groups a bulletin is meant for.

## Consequences

- The relay can authenticate, and a WhatsApp message reaches the intended recipient.
- **Neither the secret hash nor the ciphertext is exposed by any API response.** `providerResourceId` is
  exposed because a coordinator must be able to see that a WhatsApp integration has its sending
  identity; a missing one otherwise fails only at delivery.
- Deployment now requires `INTEGRATION_CREDENTIAL_KEY` before messaging integrations can be created.
  Webhook and digest integrations are unaffected.
- **Key loss means tokens must be re-entered.** There is no recovery path, which is the intended
  trade: a recoverable bot token is a token in a backup somewhere.
- `MessagingConnectorTest` now encrypts through the real cipher, so it can no longer paper over what
  the service stores. `CommunityIntegrationIT` covers create-to-provider for both channels.

## Alternatives rejected

- **Plaintext in `secret_hash`.** Fewer lines, and it works. It puts a credential that can post to a
  community's group into every dump, backup and read-only replica, under a name that tells an auditor
  it is already safe.
- **Credential references resolved from configuration** (store a name, keep the token in the
  environment). Stronger still, but it moves a per-community secret into an operator's deployment
  config, which is a worse place to keep something a community gave you.
- **Renaming `secret_hash`.** Correct eventually, but it touches the webhook path and every historical
  row for no gain now that the two columns are distinct.