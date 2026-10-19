package org.opencivic.signalos.web;

import java.security.Principal;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.service.BiasDiagnosticService;
import org.opencivic.signalos.service.CommunityAccessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bias diagnostics over data the platform holds.
 *
 * <p>Read-only, and gated on the open-data management scope rather than made public. A disparity
 * report names specific categories as treated differently, which is a claim an institution should
 * be able to review before it circulates.
 */
@RestController
@RequestMapping("/api/signals/bias-diagnostics")
public class BiasDiagnosticController {

    private final BiasDiagnosticService biasDiagnosticService;
    private final CommunityAccessService communityAccessService;

    public BiasDiagnosticController(
        BiasDiagnosticService biasDiagnosticService,
        CommunityAccessService communityAccessService
    ) {
        this.biasDiagnosticService = biasDiagnosticService;
        this.communityAccessService = communityAccessService;
    }

    @GetMapping
    public BiasDiagnosticService.BiasDiagnosticReport analyse(
        @RequestParam UUID communityId,
        Principal principal
    ) {
        var user = communityAccessService.getCurrentUser(principal.getName());
        communityAccessService.requireScope(
            user.getId(), communityId, CommunityPermissionScope.MANAGE_OPEN_DATA_EXPORTS);
        return biasDiagnosticService.analyse(communityId, principal.getName());
    }

    /** The thresholds and their version, so a reviewer can see what counts as disparate. */
    @GetMapping("/formula")
    public BiasDiagnosticService.Thresholds formula() {
        return biasDiagnosticService.defaultThresholds();
    }
}