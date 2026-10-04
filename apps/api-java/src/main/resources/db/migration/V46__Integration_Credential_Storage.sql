-- Provider credentials, stored separately from webhook signing secrets.
--
-- The relay could not authenticate as shipped: every channel's secret went through the webhook
-- hash, so a bot token was stored as a SHA-256 digest and then sent to Telegram as if it were the
-- token. The provider answers 401.
--
-- Two columns, because the two kinds of secret are not the same kind of thing:
--
--   secret_hash             webhook signing secret, hashed, never leaves the platform (nullable now
--                           because messaging rows do not use it)
--   credential_ciphertext   bot token, AES-GCM encrypted, must be sent to the provider
--
-- provider_resource_id holds the WhatsApp phone-number id, which is the sending identity in the
-- Cloud API path. It is not a secret and not a destination: the recipient stays in target_uri.

ALTER TABLE community_integrations ALTER COLUMN secret_hash DROP NOT NULL;
ALTER TABLE community_integrations ADD COLUMN credential_ciphertext TEXT NULL;
ALTER TABLE community_integrations ADD COLUMN provider_resource_id VARCHAR(64) NULL;
