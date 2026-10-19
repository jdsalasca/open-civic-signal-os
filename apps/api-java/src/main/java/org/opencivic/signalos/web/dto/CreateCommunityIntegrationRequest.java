package org.opencivic.signalos.web.dto;

import java.util.UUID;

public record CreateCommunityIntegrationRequest(
    UUID communityId,
    String channel,
    String name,
    /**
     * The destination, and its meaning depends on the channel: an absolute URL for a webhook, an
     * email address for a digest, a chat or group id for Telegram, a recipient number for WhatsApp.
     */
    String targetUri,
    /**
     * A webhook signing secret, or a bot token for the messaging channels. Stored according to the
     * channel: hashed for a webhook, encrypted for a bot token.
     */
    String secret,
    /**
     * Required for WHATSAPP, rejected for the other channels. The Cloud API path is the business
     * phone-number id, which is a different thing from the recipient in {@code targetUri}.
     */
    String providerResourceId,
    Boolean autoRetry
) {}
