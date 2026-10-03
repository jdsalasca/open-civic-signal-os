package org.opencivic.signalos.web;

import java.security.Principal;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.service.CommunityAccessService;
import org.opencivic.signalos.service.InstitutionalActionBridgeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The institutional action bridge.
 *
 * <p>Gated on the open-data management scope, because handing a resident's report to a city is an
 * act with consequences for that resident, not a read.
 */
@RestController
@RequestMapping("/api/community/institutional-handoffs")
public class InstitutionalActionBridgeController {

    private final InstitutionalActionBridgeService bridgeService;
    private final CommunityAccessService communityAccessService;

    public InstitutionalActionBridgeController(
        InstitutionalActionBridgeService bridgeService,
        CommunityAccessService communityAccessService
    ) {
        this.bridgeService = bridgeService;
        this.communityAccessService = communityAccessService;
    }

    public record OutcomeRequest(boolean resolved, String note) {}

    @PostMapping
    public InstitutionalActionBridgeService.HandoffView recordHandoff(
        @RequestBody InstitutionalActionBridgeService.HandoffRequest request,
        Principal principal
    ) {
        requireScope(request.communityId(), principal);
        return bridgeService.recordHandoff(request, principal.getName());
    }

    @PostMapping("/batch")
    public InstitutionalActionBridgeService.BatchHandoffResult recordBatchHandoff(
        @RequestBody InstitutionalActionBridgeService.BatchHandoffRequest request,
        Principal principal
    ) {
        requireScope(request.communityId(), principal);
        return bridgeService.recordBatchHandoff(request, principal.getName());
    }

    @PostMapping("/{handoffId}/outcome")
    public InstitutionalActionBridgeService.HandoffView recordOutcome(
        @PathVariable UUID handoffId,
        @RequestParam UUID communityId,
        @RequestBody OutcomeRequest request,
        Principal principal
    ) {
        requireScope(communityId, principal);
        return bridgeService.recordOutcome(handoffId, request.resolved(), request.note(), principal.getName());
    }

    @GetMapping
    public InstitutionalActionBridgeService.BridgeSummary summary(
        @RequestParam UUID communityId,
        Principal principal
    ) {
        requireScope(communityId, principal);
        return bridgeService.summary(communityId, principal.getName());
    }

    private void requireScope(UUID communityId, Principal principal) {
        var user = communityAccessService.getCurrentUser(principal.getName());
        communityAccessService.requireScope(
            user.getId(), communityId, CommunityPermissionScope.MANAGE_OPEN_DATA_EXPORTS);
    }
}