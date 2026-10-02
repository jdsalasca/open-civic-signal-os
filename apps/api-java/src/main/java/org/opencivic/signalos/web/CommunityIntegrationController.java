package org.opencivic.signalos.web;

import java.security.Principal;
import java.util.UUID;
import org.opencivic.signalos.service.CommunityIntegrationService;
import org.opencivic.signalos.web.dto.CommunityIntegrationCenterResponse;
import org.opencivic.signalos.web.dto.CommunityIntegrationDeliveryResponse;
import org.opencivic.signalos.web.dto.CommunityIntegrationFanOutResponse;
import org.opencivic.signalos.web.dto.CommunityIntegrationResponse;
import org.opencivic.signalos.web.dto.CreateCommunityIntegrationRequest;
import org.opencivic.signalos.web.dto.PublishCommunityIntegrationEventRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community/integrations")
public class CommunityIntegrationController {
    private final CommunityIntegrationService integrationService;

    public CommunityIntegrationController(CommunityIntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @GetMapping("/center")
    public CommunityIntegrationCenterResponse getCenter(
        @RequestParam UUID communityId,
        Principal principal
    ) {
        return integrationService.getCenter(communityId, principal.getName());
    }

    @PostMapping
    public CommunityIntegrationResponse createIntegration(
        @RequestBody CreateCommunityIntegrationRequest request,
        Principal principal
    ) {
        return integrationService.createIntegration(request, principal.getName());
    }

    @PatchMapping("/{integrationId}")
    public CommunityIntegrationResponse setEnabled(
        @PathVariable UUID integrationId,
        @RequestParam UUID communityId,
        @RequestParam boolean enabled,
        Principal principal
    ) {
        return integrationService.setEnabled(communityId, integrationId, enabled, principal.getName());
    }

    @PostMapping("/events")
    public CommunityIntegrationFanOutResponse publish(
        @RequestBody PublishCommunityIntegrationEventRequest request,
        Principal principal
    ) {
        return integrationService.publish(request, principal.getName());
    }

    @PostMapping("/deliveries/{deliveryId}/retry")
    public CommunityIntegrationDeliveryResponse retry(
        @PathVariable UUID deliveryId,
        @RequestParam UUID communityId,
        Principal principal
    ) {
        return integrationService.retry(communityId, deliveryId, principal.getName());
    }
}