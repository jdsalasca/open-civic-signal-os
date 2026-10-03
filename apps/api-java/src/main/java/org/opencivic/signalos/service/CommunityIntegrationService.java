package org.opencivic.signalos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityIntegration;
import org.opencivic.signalos.domain.CommunityIntegrationChannel;
import org.opencivic.signalos.domain.CommunityIntegrationDelivery;
import org.opencivic.signalos.domain.CommunityIntegrationDeliveryStatus;
import org.opencivic.signalos.domain.CommunityIntegrationEventType;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ConflictException;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityIntegrationDeliveryRepository;
import org.opencivic.signalos.repository.CommunityIntegrationRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.web.dto.CommunityIntegrationCenterResponse;
import org.opencivic.signalos.web.dto.CommunityIntegrationDeliveryResponse;
import org.opencivic.signalos.web.dto.CommunityIntegrationFanOutResponse;
import org.opencivic.signalos.web.dto.CommunityIntegrationResponse;
import org.opencivic.signalos.web.dto.CreateCommunityIntegrationRequest;
import org.opencivic.signalos.web.dto.PublishCommunityIntegrationEventRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityIntegrationService {
    static final String NO_CONNECTOR = "No connector is registered for this channel yet.";

    private final CommunityAccessService communityAccessService;
    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final CommunityIntegrationRepository integrationRepository;
    private final CommunityIntegrationDeliveryRepository deliveryRepository;
    private final List<CommunityIntegrationConnector> connectors;
    private final ObjectMapper objectMapper;

    public CommunityIntegrationService(
        CommunityAccessService communityAccessService,
        CommunityRepository communityRepository,
        UserRepository userRepository,
        CommunityIntegrationRepository integrationRepository,
        CommunityIntegrationDeliveryRepository deliveryRepository,
        List<CommunityIntegrationConnector> connectors,
        ObjectMapper objectMapper
    ) {
        this.communityAccessService = communityAccessService;
        this.communityRepository = communityRepository;
        this.userRepository = userRepository;
        this.integrationRepository = integrationRepository;
        this.deliveryRepository = deliveryRepository;
        this.connectors = connectors;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public CommunityIntegrationCenterResponse getCenter(UUID communityId, String username, Integer limit) {
        User user = communityAccessService.getCurrentUser(username);
        communityAccessService.requireScope(user.getId(), communityId, CommunityPermissionScope.MANAGE_INTEGRATIONS);
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));
        int max = CommunityListLimits.resolveLimit(limit);

        Map<UUID, CommunityIntegration> byId = new LinkedHashMap<>();
        List<CommunityIntegration> integrations =
            integrationRepository.findByCommunityIdOrderByCreatedAtDesc(communityId);
        integrations.forEach(integration -> byId.put(integration.getId(), integration));

        List<CommunityIntegrationDelivery> deliveries =
            deliveryRepository.findByCommunityIdOrderByCreatedAtDesc(communityId);

        return new CommunityIntegrationCenterResponse(
            community.getId(),
            community.getName(),
            List.of(CommunityIntegrationChannel.values()).stream().map(Enum::name).toList(),
            integrations.size(),
            deliveryRepository.countByCommunityIdAndStatus(communityId, CommunityIntegrationDeliveryStatus.PENDING),
            deliveryRepository.countByCommunityIdAndStatus(communityId, CommunityIntegrationDeliveryStatus.FAILED),
            integrations.stream()
                .limit(max)
                .map(integration -> toResponse(integration, deliveries))
                .toList(),
            deliveries.stream()
                .limit(30)
                .map(delivery -> toDeliveryResponse(delivery, byId))
                .toList()
        );
    }

    @Transactional
    public CommunityIntegrationResponse createIntegration(
        CreateCommunityIntegrationRequest request,
        String username
    ) {
        User user = communityAccessService.getCurrentUser(username);
        if (request.communityId() == null) {
            throw new IllegalArgumentException("communityId is required to create an integration.");
        }
        communityAccessService.requireScope(
            user.getId(), request.communityId(), CommunityPermissionScope.MANAGE_INTEGRATIONS
        );

        if (isBlank(request.name()) || request.name().trim().length() < 3) {
            throw new IllegalArgumentException("Integration name must be at least 3 characters.");
        }
        if (isBlank(request.targetUri()) || !isHttpUri(request.targetUri().trim())) {
            throw new IllegalArgumentException("targetUri must be an absolute http or https URL.");
        }
        if (isBlank(request.secret()) || request.secret().trim().length() < 8) {
            throw new IllegalArgumentException("A signing secret of at least 8 characters is required.");
        }

        CommunityIntegrationChannel channel;
        try {
            channel = CommunityIntegrationChannel.valueOf(request.channel().trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Unknown integration channel: " + request.channel());
        }

        CommunityIntegration integration = new CommunityIntegration();
        integration.setCommunityId(request.communityId());
        integration.setChannel(channel);
        integration.setName(request.name().trim());
        integration.setTargetUri(request.targetUri().trim());
        integration.setSecretHash(WebhookCommunityIntegrationConnector.hashSecret(request.secret().trim()));
        integration.setAutoRetry(request.autoRetry() == null || request.autoRetry());
        integration.setCreatedBy(user.getId());
        integration = integrationRepository.save(integration);

        return toResponse(integration, List.of());
    }

    @Transactional
    public CommunityIntegrationResponse setEnabled(
        UUID communityId,
        UUID integrationId,
        boolean enabled,
        String username
    ) {
        User user = communityAccessService.getCurrentUser(username);
        communityAccessService.requireScope(user.getId(), communityId, CommunityPermissionScope.MANAGE_INTEGRATIONS);
        CommunityIntegration integration = requireIntegration(communityId, integrationId);

        integration.setEnabled(enabled);
        integrationRepository.save(integration);

        return toResponse(integration, List.of());
    }

    @Transactional
    public CommunityIntegrationFanOutResponse publish(
        PublishCommunityIntegrationEventRequest request,
        String username
    ) {
        User user = communityAccessService.getCurrentUser(username);
        communityAccessService.requireScope(
            user.getId(), request.communityId(), CommunityPermissionScope.MANAGE_INTEGRATIONS
        );

        CommunityIntegrationEventType eventType;
        try {
            eventType = CommunityIntegrationEventType.valueOf(request.eventType().trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Unknown integration event type: " + request.eventType());
        }
        if (eventType == CommunityIntegrationEventType.ACTIVITY_SCHEDULED
            || eventType == CommunityIntegrationEventType.RESOURCE_BOOKED) {
            if (request.startsAt() == null || request.endsAt() == null) {
                throw new IllegalArgumentException("Scheduled events require startsAt and endsAt.");
            }
            if (!request.endsAt().isAfter(request.startsAt())) {
                throw new IllegalArgumentException("Event end time must be after its start time.");
            }
        }

        List<CommunityIntegration> targets = resolveTargets(request.communityId(), eventType);
        List<CommunityIntegrationDeliveryResponse> responses = targets.stream()
            .map(integration -> dispatch(integration, eventType, request))
            .toList();

        return new CommunityIntegrationFanOutResponse(
            request.communityId(),
            eventType.name(),
            targets.size(),
            (int) responses.stream().filter(r -> "DELIVERED".equals(r.status())).count(),
            (int) responses.stream().filter(r -> "FAILED".equals(r.status())).count(),
            (int) responses.stream().filter(r -> NO_CONNECTOR.equals(r.lastError())).count(),
            responses
        );
    }

    @Transactional
    public CommunityIntegrationDeliveryResponse retry(
        UUID communityId,
        UUID deliveryId,
        String username
    ) {
        User user = communityAccessService.getCurrentUser(username);
        communityAccessService.requireScope(user.getId(), communityId, CommunityPermissionScope.MANAGE_INTEGRATIONS);

        CommunityIntegrationDelivery delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow(() -> new ResourceNotFoundException("Delivery not found: " + deliveryId));
        if (!delivery.getCommunityId().equals(communityId)) {
            throw new ResourceNotFoundException("Delivery not found: " + deliveryId);
        }
        if (!delivery.isRetryable()) {
            throw new ConflictException("Only failed deliveries can be retried.");
        }

        CommunityIntegration integration = requireIntegration(communityId, delivery.getIntegrationId());
        if (!integration.isAutoRetry()) {
            throw new ConflictException("This integration has automatic retries disabled.");
        }

        String body = delivery.getPayload();
        CommunityIntegrationConnector.DeliveryResult result = dispatchResult(integration, body);

        recordAttempt(integration, result);
        delivery.setStatus(result.delivered()
            ? CommunityIntegrationDeliveryStatus.DELIVERED
            : CommunityIntegrationDeliveryStatus.FAILED);
        delivery.setAttempts(delivery.getAttempts() + 1);
        delivery.setLastError(result.error());
        delivery.setCompletedAt(LocalDateTime.now());
        deliveryRepository.save(delivery);

        return toDeliveryResponse(delivery, Map.of(integration.getId(), integration));
    }

    private List<CommunityIntegration> resolveTargets(UUID communityId, CommunityIntegrationEventType eventType) {
        return integrationRepository.findByCommunityIdOrderByCreatedAtDesc(communityId).stream()
            .filter(CommunityIntegration::isEnabled)
            .filter(integration -> acceptsEvent(integration.getChannel(), eventType))
            .toList();
    }

    /**
     * Which events each channel carries.
     *
     * <p>Stated per channel rather than "everything except calendar". A default of yes meant that
     * adding the email channel would have sent every official announcement to residents as a weekly
     * bulletin, which is the kind of thing nobody notices until someone complains about the mail.
     *
     * @param channel   the configured transport
     * @param eventType the event being published
     * @return whether this channel should receive this event
     */
    static boolean acceptsEvent(CommunityIntegrationChannel channel, CommunityIntegrationEventType eventType) {
        return switch (channel) {
            // A calendar feed carries scheduled events, and nothing else.
            case CALENDAR_FEED -> eventType == CommunityIntegrationEventType.ACTIVITY_SCHEDULED
                || eventType == CommunityIntegrationEventType.RESOURCE_BOOKED;
            // An email digest carries the digest and nothing else. Announcements have their own
            // channel and residents did not sign up for one to receive the other.
            case EMAIL_DIGEST -> eventType == CommunityIntegrationEventType.WEEKLY_DIGEST;
            // A webhook is the general-purpose transport and receives everything.
            case WEBHOOK -> true;
            // A map link has no connector. It is deliberately NOT filtered out here: filtering would
            // make a configured-but-unservable integration look idle, and this layer's documented
            // behaviour is that such a channel fails visibly with NO_CONNECTOR instead.
            case MAP_LINK -> true;
        };
    }

    /**
     * Finds the connector for a channel, or reports the miss.
     *
     * <p>A channel nobody supports returns {@code NO_CONNECTOR} rather than silently doing nothing,
     * which is the documented behaviour of this layer: an integration a community configured but the
     * platform cannot serve must look broken, not look idle.
     */
    private CommunityIntegrationConnector.DeliveryResult dispatchResult(
        CommunityIntegration integration,
        String body
    ) {
        return connectors.stream()
            .filter(candidate -> candidate.supports(integration.getChannel()))
            .findFirst()
            .map(connector -> connector.deliver(integration, body))
            .orElseGet(() -> new CommunityIntegrationConnector.DeliveryResult(false, NO_CONNECTOR, 0));
    }

    /**
     * Fans a rendered digest out to every enabled email integration for a community.
     *
     * <p>Called after a digest is published, not on a schedule. Idempotency comes from the digest
     * publication itself: a week can only be published once, so a retry cannot reach this twice for
     * the same week.
     *
     * <p>Partial failure is expected and recoverable: each delivery is recorded, and the existing
     * retry endpoint re-sends a failed one without republishing the week.
     */
    @Transactional
    public int fanOutDigest(UUID communityId, UUID publicationId, String body) {
        List<CommunityIntegration> targets = resolveTargets(
            communityId, CommunityIntegrationEventType.WEEKLY_DIGEST);
        int delivered = 0;
        for (CommunityIntegration target : targets) {
            CommunityIntegrationDelivery delivery = new CommunityIntegrationDelivery();
            delivery.setIntegrationId(target.getId());
            delivery.setCommunityId(target.getCommunityId());
            delivery.setEventType(CommunityIntegrationEventType.WEEKLY_DIGEST);
            // The publication id, so a delivery can be traced back to the sealed digest it carried.
            delivery.setReferenceId(publicationId);
            delivery.setPayload(body);
            delivery.setStatus(CommunityIntegrationDeliveryStatus.PENDING);
            delivery = deliveryRepository.save(delivery);

            CommunityIntegrationConnector.DeliveryResult result = dispatchResult(target, body);
            recordAttempt(target, result);
            delivery.setStatus(result.delivered()
                ? CommunityIntegrationDeliveryStatus.DELIVERED
                : CommunityIntegrationDeliveryStatus.FAILED);
            delivery.setAttempts(delivery.getAttempts() + 1);
            delivery.setLastError(result.error());
            delivery.setCompletedAt(LocalDateTime.now());
            deliveryRepository.save(delivery);
            if (result.delivered()) {
                delivered++;
            }
        }
        return delivered;
    }

    private CommunityIntegrationDeliveryResponse dispatch(
        CommunityIntegration integration,
        CommunityIntegrationEventType eventType,
        PublishCommunityIntegrationEventRequest request
    ) {
        CommunityIntegrationDelivery delivery = new CommunityIntegrationDelivery();
        delivery.setIntegrationId(integration.getId());
        delivery.setCommunityId(integration.getCommunityId());
        delivery.setEventType(eventType);
        delivery.setReferenceId(request.referenceId());
        delivery.setPayload("");
        delivery.setStatus(CommunityIntegrationDeliveryStatus.PENDING);
        delivery = deliveryRepository.save(delivery);

        String body = buildBody(integration, eventType, request);
        delivery.setPayload(body);

        CommunityIntegrationConnector.DeliveryResult result = dispatchResult(integration, body);

        recordAttempt(integration, result);
        delivery.setStatus(result.delivered()
            ? CommunityIntegrationDeliveryStatus.DELIVERED
            : CommunityIntegrationDeliveryStatus.FAILED);
        delivery.setAttempts(delivery.getAttempts() + 1);
        delivery.setLastError(result.error());
        delivery.setCompletedAt(LocalDateTime.now());
        delivery = deliveryRepository.save(delivery);

        return toDeliveryResponse(delivery, Map.of(integration.getId(), integration));
    }

    private void recordAttempt(
        CommunityIntegration integration,
        CommunityIntegrationConnector.DeliveryResult result
    ) {
        integration.setLastAttemptAt(LocalDateTime.now());
        if (result.delivered()) {
            integration.setLastSuccessAt(LocalDateTime.now());
            integration.setConsecutiveFailures(0);
        } else {
            integration.setConsecutiveFailures(integration.getConsecutiveFailures() + 1);
        }
        integrationRepository.save(integration);
    }

    private String buildBody(
        CommunityIntegration integration,
        CommunityIntegrationEventType eventType,
        PublishCommunityIntegrationEventRequest request
    ) {
        if (integration.getChannel() == CommunityIntegrationChannel.CALENDAR_FEED) {
            return WebhookCommunityIntegrationConnector.calendarPayload(
                (request.referenceId() == null ? integration.getId() : request.referenceId()).toString(),
                request.title(),
                request.description(),
                request.locationLabel(),
                request.startsAt(),
                request.endsAt()
            );
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", eventType.name());
        payload.put("communityId", request.communityId().toString());
        payload.put("referenceId", request.referenceId() == null ? null : request.referenceId().toString());
        payload.put("title", request.title());
        payload.put("description", request.description());
        payload.put("locationLabel", request.locationLabel());
        payload.put("startsAt", request.startsAt() == null ? null : request.startsAt().toString());
        payload.put("endsAt", request.endsAt() == null ? null : request.endsAt().toString());
        payload.put("emittedAt", LocalDateTime.now().toString());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to serialize integration payload: " + e.getMessage());
        }
    }

    private CommunityIntegrationResponse toResponse(
        CommunityIntegration integration,
        List<CommunityIntegrationDelivery> deliveries
    ) {
        List<CommunityIntegrationDelivery> own = deliveries.stream()
            .filter(delivery -> delivery.getIntegrationId().equals(integration.getId()))
            .toList();
        return new CommunityIntegrationResponse(
            integration.getId(),
            integration.getCommunityId(),
            integration.getChannel().name(),
            integration.getName(),
            integration.getTargetUri(),
            integration.isEnabled(),
            integration.isAutoRetry(),
            connectors.stream().anyMatch(candidate -> candidate.supports(integration.getChannel())),
            integration.getCreatedBy(),
            integration.getCreatedAt(),
            integration.getLastAttemptAt(),
            integration.getLastSuccessAt(),
            integration.getConsecutiveFailures(),
            own.stream().filter(d -> d.getStatus() == CommunityIntegrationDeliveryStatus.PENDING).count(),
            own.stream().filter(d -> d.getStatus() == CommunityIntegrationDeliveryStatus.FAILED).count()
        );
    }

    private CommunityIntegrationDeliveryResponse toDeliveryResponse(
        CommunityIntegrationDelivery delivery,
        Map<UUID, CommunityIntegration> integrationsById
    ) {
        CommunityIntegration integration = integrationsById.get(delivery.getIntegrationId());
        return new CommunityIntegrationDeliveryResponse(
            delivery.getId(),
            delivery.getIntegrationId(),
            integration == null ? "unknown" : integration.getName(),
            integration == null ? "UNKNOWN" : integration.getChannel().name(),
            delivery.getEventType().name(),
            delivery.getReferenceId(),
            delivery.getStatus().name(),
            delivery.getAttempts(),
            delivery.getLastError(),
            delivery.getCreatedAt(),
            delivery.getCompletedAt()
        );
    }

    private CommunityIntegration requireIntegration(UUID communityId, UUID integrationId) {
        CommunityIntegration integration = integrationRepository.findById(integrationId)
            .orElseThrow(() -> new ResourceNotFoundException("Integration not found: " + integrationId));
        if (!integration.getCommunityId().equals(communityId)) {
            throw new ResourceNotFoundException("Integration not found: " + integrationId);
        }
        return integration;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean isHttpUri(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }
}