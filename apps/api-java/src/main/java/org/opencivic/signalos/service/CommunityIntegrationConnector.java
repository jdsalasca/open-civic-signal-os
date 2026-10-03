package org.opencivic.signalos.service;

import org.opencivic.signalos.domain.CommunityIntegration;
import org.opencivic.signalos.domain.CommunityIntegrationChannel;

/**
 * A transport that can deliver an integration payload somewhere.
 *
 * <p>Introduced when the digest needed an email channel. The webhook connector used to be injected
 * directly as a concrete class, which meant a second channel had nowhere to plug in: the service
 * called one implementation rather than asking which one handles a channel.
 *
 * <p>{@link #supports} is the whole contract for selection. A channel nobody supports must fail
 * visibly with {@code NO_CONNECTOR} rather than silently doing nothing, which is why
 * {@code CommunityIntegrationService} looks the connector up and reports the miss instead of
 * defaulting to one.
 */
public interface CommunityIntegrationConnector {

    /**
     * Outcome of one delivery attempt.
     *
     * @param delivered  whether the payload reached the target
     * @param error      a short, truncated reason when it did not
     * @param statusCode transport status when the transport has one; 0 for transports that do not
     */
    record DeliveryResult(boolean delivered, String error, int statusCode) {}

    boolean supports(CommunityIntegrationChannel channel);

    DeliveryResult deliver(CommunityIntegration integration, String body);
}
