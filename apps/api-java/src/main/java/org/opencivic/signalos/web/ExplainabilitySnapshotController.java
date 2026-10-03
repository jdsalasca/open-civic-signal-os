package org.opencivic.signalos.web;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.service.ExplainabilitySnapshotService;
import org.opencivic.signalos.web.dto.ExplainabilitySnapshotCreateRequest;
import org.opencivic.signalos.web.dto.ExplainabilitySnapshotResponse;
import org.opencivic.signalos.web.dto.ExplainabilitySnapshotVerificationResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Explainability snapshots for assemblies.
 *
 * <p>Read-only after creation apart from verification: there is deliberately no update or
 * delete endpoint, because a snapshot that can be edited is not evidence.
 */
@RestController
@RequestMapping("/api/community/explainability-snapshots")
public class ExplainabilitySnapshotController {

    private final ExplainabilitySnapshotService snapshotService;

    public ExplainabilitySnapshotController(ExplainabilitySnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @PostMapping
    public ExplainabilitySnapshotResponse create(
        @Valid @RequestBody ExplainabilitySnapshotCreateRequest request,
        Principal principal
    ) {
        return snapshotService.create(request, principal.getName());
    }

    @GetMapping
    public List<ExplainabilitySnapshotResponse> list(
        @RequestParam UUID communityId,
        Principal principal
    ) {
        return snapshotService.list(communityId, principal.getName());
    }

    @GetMapping("/{snapshotId}")
    public ExplainabilitySnapshotResponse get(
        @PathVariable UUID snapshotId,
        @RequestParam UUID communityId,
        Principal principal
    ) {
        return snapshotService.get(snapshotId, communityId, principal.getName());
    }

    @GetMapping("/{snapshotId}/verification")
    public ExplainabilitySnapshotVerificationResponse verify(
        @PathVariable UUID snapshotId,
        @RequestParam UUID communityId,
        Principal principal
    ) {
        return snapshotService.verify(snapshotId, communityId, principal.getName());
    }
}