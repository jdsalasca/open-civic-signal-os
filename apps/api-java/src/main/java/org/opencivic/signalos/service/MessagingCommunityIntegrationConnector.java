package org.opencivic.signalos.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.opencivic.signalos.domain.CommunityIntegration;
import org.opencivic.signalos.domain.CommunityIntegrationChannel;
import org.springframework.stereotype.Component;

/**
 * Delivers a message to a WhatsApp or Telegram group through a bot the community owns.
 *
 * <p>The bot token is the integration's {@code secretHash} field, which for these channels holds the
 * token itself rather than a hash. That is a deliberate departure from the webhook channel, where the
 * secret signs a payload and never leaves the platform. Here the token has to be sent to the
 * provider, so it cannot be hashed, and the field name is a lie for these two channels.
 *
 * <p>Two things this does not do:
 *
 * <ul>
 *   <li><b>It does not use a platform-owned bot.</b> A platform bot would make every message come
 *       from the platform, and a resident replying to it would reach nobody who can act. The
 *       community's own bot means the reply goes to the community.
 *   <li><b>It does not retry inside the connector.</b> The integration layer already records attempts
 *       and has a retry endpoint; a second retry loop here would double-send on a slow failure.
 * </ul>
 */
@Component
public class MessagingCommunityIntegrationConnector implements CommunityIntegrationConnector {

    static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);

    private static final String DEFAULT_TELEGRAM_API = "https://api.telegram.org";
    private static final String DEFAULT_WHATSAPP_API = "https://graph.facebook.com/v21.0";
    private static final int MAX_MESSAGE_LENGTH = 4000;

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(REQUEST_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();

    private final String telegramApi;
    private final String whatsappApi;

    public MessagingCommunityIntegrationConnector(
        @org.springframework.beans.factory.annotation.Value("${app.messaging.telegram-api:}")
        String telegramApiOverride,
        @org.springframework.beans.factory.annotation.Value("${app.messaging.whatsapp-api:}")
        String whatsappApiOverride
    ) {
        // Overridable so the connector can be exercised against a stub. Without this the only way to
        // test it is to call the real providers, which a test must never do.
        this.telegramApi = telegramApiOverride == null || telegramApiOverride.isBlank()
            ? DEFAULT_TELEGRAM_API : telegramApiOverride;
        this.whatsappApi = whatsappApiOverride == null || whatsappApiOverride.isBlank()
            ? DEFAULT_WHATSAPP_API : whatsappApiOverride;
    }

    @Override
    public boolean supports(CommunityIntegrationChannel channel) {
        return channel == CommunityIntegrationChannel.WHATSAPP
            || channel == CommunityIntegrationChannel.TELEGRAM;
    }

    @Override
    public DeliveryResult deliver(CommunityIntegration integration, String body) {
        String token = integration.getSecretHash();
        if (token == null || token.isBlank()) {
            return new DeliveryResult(false,
                "No bot token configured for this " + integration.getChannel().name()
                    + " integration. The community must supply its own bot so replies reach someone "
                    + "who can act.", 0);
        }
        String chatId = integration.getTargetUri() == null ? "" : integration.getTargetUri().trim();
        if (chatId.isBlank()) {
            return new DeliveryResult(false,
                "No chat or group id configured for this " + integration.getChannel().name()
                    + " integration.", 0);
        }
        if (body == null || body.isBlank()) {
            // A blank message reaching a group is worse than a failed one.
            return new DeliveryResult(false, "Refusing to send an empty message.", 0);
        }

        String message = body.length() > MAX_MESSAGE_LENGTH
            ? body.substring(0, MAX_MESSAGE_LENGTH - 3) + "..."
            : body;

        return integration.getChannel() == CommunityIntegrationChannel.TELEGRAM
            ? deliverTelegram(token, chatId, message)
            : deliverWhatsApp(token, chatId, message);
    }

    /**
     * Telegram Bot API: a JSON POST to {@code /bot<token>/sendMessage}.
     *
     * <p>The token is in the path, which is how the API works. It means the token appears in any
     * request log along the way, and that is a property of the provider rather than of this code.
     */
    private DeliveryResult deliverTelegram(String token, String chatId, String message) {
        String payload = "{\"chat_id\":\"" + escapeJson(chatId) + "\",\"text\":\"" + escapeJson(message) + "\"}";
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(telegramApi + "/bot" + token + "/sendMessage"))
            .timeout(REQUEST_TIMEOUT)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
            .build();
        return send(request);
    }

    /**
     * WhatsApp Cloud API: a form POST to {@code /<phoneNumberId>/messages}.
     *
     * <p>The token goes in the Authorization header rather than the path, which is better, and the
     * chat id is the recipient number.
     */
    private DeliveryResult deliverWhatsApp(String token, String chatId, String message) {
        String form = "messaging_product=whatsapp"
            + "&to=" + URLEncoder.encode(chatId, StandardCharsets.UTF_8)
            + "&type=text"
            + "&text[body]=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(whatsappApi + "/" + chatId + "/messages"))
            .timeout(REQUEST_TIMEOUT)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Authorization", "Bearer " + token)
            .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
            .build();
        return send(request);
    }

    private DeliveryResult send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return new DeliveryResult(true, null, response.statusCode());
            }
            // The provider's body carries the reason, and it is the only place the reason exists.
            return new DeliveryResult(false,
                "Provider returned HTTP " + response.statusCode() + ": " + truncate(response.body()),
                response.statusCode());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new DeliveryResult(false, "Delivery interrupted.", 0);
        } catch (Exception ex) {
            return new DeliveryResult(false,
                truncate(ex.getClass().getSimpleName() + ": " + ex.getMessage()), 0);
        }
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 280 ? value : value.substring(0, 277) + "...";
    }
}