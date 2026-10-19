package org.opencivic.signalos.domain;

public enum CommunityIntegrationChannel {
    WEBHOOK,
    EMAIL_DIGEST,
    CALENDAR_FEED,
    MAP_LINK,
    /**
     * A WhatsApp group or channel, reached through a bot the community owns.
     *
     * <p>The bot token belongs to the community rather than the platform, so it is clear who is
     * sending. A platform-owned bot would make every message come from the platform, and a resident
     * replying to it would reach nobody who can act.
     */
    WHATSAPP,
    /** A Telegram group or channel, reached through a bot the community owns. */
    TELEGRAM
}