package org.opencivic.signalos.web;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.service.CommunityAccessService;
import org.opencivic.signalos.service.CommunityAssemblyService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Assembly mode.
 *
 * <p>Gated on the open-data management scope: convening an assembly and recording its decisions is a
 * governance act, not a read.
 */
@RestController
@RequestMapping("/api/community/assemblies")
public class CommunityAssemblyController {

    private final CommunityAssemblyService assemblyService;
    private final CommunityAccessService communityAccessService;

    public CommunityAssemblyController(
        CommunityAssemblyService assemblyService,
        CommunityAccessService communityAccessService
    ) {
        this.assemblyService = assemblyService;
        this.communityAccessService = communityAccessService;
    }

    public record CreateRequest(
        UUID communityId,
        String title,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledFor,
        String location
    ) {}

    public record CloseRequest(String minutes) {}

    @PostMapping
    public CommunityAssemblyService.AssemblyView create(
        @RequestBody CreateRequest request,
        Principal principal
    ) {
        requireScope(request.communityId(), principal);
        return assemblyService.createAssembly(
            new CommunityAssemblyService.CreateAssemblyRequest(
                request.communityId(), request.title(), request.scheduledFor(), request.location()),
            principal.getName());
    }

    @PostMapping("/{assemblyId}/evidence")
    public CommunityAssemblyService.AssemblyView attachEvidence(
        @PathVariable UUID assemblyId,
        @RequestParam UUID communityId,
        @RequestParam UUID snapshotId,
        Principal principal
    ) {
        requireScope(communityId, principal);
        return assemblyService.attachEvidence(assemblyId, communityId, snapshotId, principal.getName());
    }

    @PostMapping("/{assemblyId}/open")
    public CommunityAssemblyService.AssemblyView open(
        @PathVariable UUID assemblyId,
        @RequestParam UUID communityId,
        Principal principal
    ) {
        requireScope(communityId, principal);
        return assemblyService.openAssembly(assemblyId, communityId, principal.getName());
    }

    @PostMapping("/{assemblyId}/close")
    public CommunityAssemblyService.AssemblyView close(
        @PathVariable UUID assemblyId,
        @RequestParam UUID communityId,
        @RequestBody(required = false) CloseRequest request,
        Principal principal
    ) {
        requireScope(communityId, principal);
        return assemblyService.closeAssembly(
            assemblyId, communityId, request == null ? null : request.minutes(), principal.getName());
    }

    @PostMapping("/{assemblyId}/decisions")
    public CommunityAssemblyService.AssemblyView recordDecision(
        @PathVariable UUID assemblyId,
        @RequestParam UUID communityId,
        @RequestBody CommunityAssemblyService.RecordDecisionRequest request,
        Principal principal
    ) {
        requireScope(communityId, principal);
        return assemblyService.recordDecision(assemblyId, communityId, request, principal.getName());
    }

    @GetMapping("/{assemblyId}")
    public CommunityAssemblyService.AssemblyView get(
        @PathVariable UUID assemblyId,
        @RequestParam UUID communityId,
        Principal principal
    ) {
        requireScope(communityId, principal);
        return assemblyService.getAssembly(assemblyId, communityId, principal.getName());
    }

    @GetMapping
    public List<CommunityAssemblyService.AssemblyView> list(
        @RequestParam UUID communityId,
        Principal principal
    ) {
        requireScope(communityId, principal);
        return assemblyService.listAssemblies(communityId, principal.getName());
    }

    private void requireScope(UUID communityId, Principal principal) {
        var user = communityAccessService.getCurrentUser(principal.getName());
        communityAccessService.requireScope(
            user.getId(), communityId, CommunityPermissionScope.MANAGE_OPEN_DATA_EXPORTS);
    }
}