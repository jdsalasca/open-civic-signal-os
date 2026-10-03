package org.opencivic.signalos.web;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.service.CommunityAccessService;
import org.opencivic.signalos.service.CommunityTrustPulseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The community trust pulse.
 *
 * <p>Submitting requires membership: a pulse is a community's own opinion, and someone outside it
 * has no standing to answer. Reading the aggregate also requires membership, because a community's
 * opinion of its platform is not privileged information but it is not the whole world's either.
 */
@RestController
@RequestMapping("/api/community/trust-pulse")
public class CommunityTrustPulseController {

    private final CommunityTrustPulseService pulseService;
    private final CommunityAccessService communityAccessService;

    public CommunityTrustPulseController(
        CommunityTrustPulseService pulseService,
        CommunityAccessService communityAccessService
    ) {
        this.pulseService = pulseService;
        this.communityAccessService = communityAccessService;
    }

    @PostMapping
    public CommunityTrustPulseService.PulseSubmissionResult submit(
        @RequestBody CommunityTrustPulseService.PulseSubmission submission,
        Principal principal
    ) {
        requireMembership(submission.communityId(), principal);
        return pulseService.submit(submission, principal.getName());
    }

    @GetMapping
    public CommunityTrustPulseService.PulseAggregate aggregate(
        @RequestParam UUID communityId,
        @RequestParam(required = false) String period,
        Principal principal
    ) {
        requireMembership(communityId, principal);
        return pulseService.aggregate(communityId, period, principal.getName());
    }

    @GetMapping("/history")
    public List<CommunityTrustPulseService.PulseAggregate> history(
        @RequestParam UUID communityId,
        Principal principal
    ) {
        requireMembership(communityId, principal);
        return pulseService.history(communityId, principal.getName());
    }

    /** The floor below which no average is reported, so a client can explain it before submitting. */
    @GetMapping("/minimum-sample")
    public int minimumSample() {
        return pulseService.minimumResponsesForAverage();
    }

    private void requireMembership(UUID communityId, Principal principal) {
        var user = communityAccessService.getCurrentUser(principal.getName());
        communityAccessService.requireMembership(user.getId(), communityId);
    }
}