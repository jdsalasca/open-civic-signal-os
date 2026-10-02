package org.opencivic.signalos.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.opencivic.signalos.domain.CommunityIntegration;
import org.opencivic.signalos.domain.CommunityIntegrationChannel;
import org.springframework.stereotype.Component;

/**
 * Delivers integration payloads to an external endpoint over HTTP.
 *
 * Both {@code WEBHOOK} and {@code CALENDAR_FEED} are HTTP transports: a calendar feed
 * is an HTTP endpoint that serves {@code text/calendar}, so they share one client and
 * differ only in the payload and content type.
 *
 * ponytail: synchronous delivery with a short timeout. A community-scale fan-out is fine
 * here; move to a queue + worker before integrations number in the hundreds or targets
 * are slower than {@link #REQUEST_TIMEOUT}.
 */
@Component
public class WebhookCommunityIntegrationConnector {
    static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(REQUEST_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();

    public record DeliveryResult(boolean delivered, String error, int statusCode) {}

    public boolean supports(CommunityIntegrationChannel channel) {
        return channel == CommunityIntegrationChannel.WEBHOOK
            || channel == CommunityIntegrationChannel.CALENDAR_FEED;
    }

    public String contentType(CommunityIntegrationChannel channel) {
        return channel == CommunityIntegrationChannel.CALENDAR_FEED
            ? "text/calendar; charset=utf-8"
            : "application/json";
    }

    public DeliveryResult deliver(CommunityIntegration integration, String body) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(integration.getTargetUri()))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", contentType(integration.getChannel()))
                .header("X-Open-Civic-Integration", integration.getName())
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

            HttpResponse<String> response = httpClient.send(
                request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return new DeliveryResult(true, null, response.statusCode());
            }
            return new DeliveryResult(false, "Endpoint returned HTTP " + response.statusCode(), response.statusCode());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new DeliveryResult(false, "Delivery interrupted.", 0);
        } catch (Exception e) {
            return new DeliveryResult(false, truncate(e.getClass().getSimpleName() + ": " + e.getMessage()), 0);
        }
    }

    /**
     * HMAC-SHA256 over the raw body so receivers can prove the sender and detect tampering.
     * The integration only stores the secret hash, so this needs the plaintext secret.
     */
    public static String sign(String secret, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign integration payload", e);
        }
    }

    public static String hashSecret(String secret) {
        return sign(secret, secret);
    }

    static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 280 ? value : value.substring(0, 277) + "...";
    }

    static String calendarPayload(String uid, String title, String description, String location,
                                  java.time.LocalDateTime startsAt, java.time.LocalDateTime endsAt) {
        java.time.format.DateTimeFormatter stamp = java.time.format.DateTimeFormatter
            .ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(java.time.ZoneOffset.UTC);
        return """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Open Civic Signal OS//Civic//EN
            BEGIN:VEVENT
            UID:%s
            DTSTAMP:%s
            DTSTART:%s
            DTEND:%s
            SUMMARY:%s
            DESCRIPTION:%s
            LOCATION:%s
            END:VEVENT
            END:VCALENDAR
            """.formatted(
                uid,
                stamp.format(java.time.Instant.now()),
                stamp.format(startsAt.toInstant(java.time.ZoneOffset.UTC)),
                stamp.format(endsAt.toInstant(java.time.ZoneOffset.UTC)),
                escapeIcs(title),
                escapeIcs(description),
                escapeIcs(location)
            );
    }

    private static String escapeIcs(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace(",", "\\,")
            .replace(";", "\\;");
    }
}