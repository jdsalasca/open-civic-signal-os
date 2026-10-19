package org.opencivic.signalos.service;

import java.util.regex.Pattern;
import org.opencivic.signalos.domain.CommunityIntegration;
import org.opencivic.signalos.domain.CommunityIntegrationChannel;
import org.springframework.stereotype.Component;

/**
 * Delivers a digest to an email address using the platform's audited mail path.
 *
 * <p>The recipient is the integration's {@code targetUri}, which a community configures. That means
 * this can send mail to addresses a community supplies, on a scope those communities already hold
 * for webhooks. The address is validated before use: an integration misconfigured with a URL would
 * otherwise produce a confusing transport error rather than a clear one.
 *
 * <p>Deliberately does not compose the body. The digest is the artifact that has to be reproducible
 * and sealed, and it arrives already rendered.
 */
@Component
public class EmailDigestCommunityIntegrationConnector implements CommunityIntegrationConnector {

    // Intentionally permissive: the mail transport is the authority on deliverability, and a strict
    // pattern here would reject valid addresses without improving anything. This only catches a
    // value that is obviously not an address at all.
    private static final Pattern LOOKS_LIKE_ADDRESS = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private static final String SUBJECT = "Your community's week on Open Civic Signal OS";

    private final EmailService emailService;

    public EmailDigestCommunityIntegrationConnector(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public boolean supports(CommunityIntegrationChannel channel) {
        return channel == CommunityIntegrationChannel.EMAIL_DIGEST;
    }

    @Override
    public DeliveryResult deliver(CommunityIntegration integration, String body) {
        String to = integration.getTargetUri() == null ? "" : integration.getTargetUri().trim();
        if (!LOOKS_LIKE_ADDRESS.matcher(to).matches()) {
            return new DeliveryResult(false,
                "EMAIL_DIGEST target is not an email address: " + truncate(to), 0);
        }
        if (body == null || body.isBlank()) {
            // A blank bulletin reaching residents is worse than a failed one, and the digest service
            // should never produce it. Refusing here means a bug upstream surfaces as a failed
            // delivery rather than as an empty email someone has to explain.
            return new DeliveryResult(false, "Refusing to send an empty digest body.", 0);
        }

        EmailDeliveryResult result = emailService.sendDigest(to, SUBJECT, body);
        return new DeliveryResult(result.delivered(), result.failureReason(), 0);
    }

    private String truncate(String value) {
        return value.length() <= 200 ? value : value.substring(0, 197) + "...";
    }
}
