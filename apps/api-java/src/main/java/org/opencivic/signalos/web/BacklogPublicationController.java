package org.opencivic.signalos.web;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.service.BacklogPublicationService;
import org.opencivic.signalos.service.CommunityAccessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Publishing the public backlog and checking it.
 *
 * <p>`POST /publish` and the history are gated: publishing declares what the public sees, which is a
 * moderation act rather than a read. `GET /current` and `GET /verify` are public, because a claim
 * only a maintainer can check is not a claim a visitor can trust.
 */
@RestController
@RequestMapping("/api/community/backlog-publications")
public class BacklogPublicationController {

    private final BacklogPublicationService publicationService;
    private final CommunityAccessService communityAccessService;

    public BacklogPublicationController(
        BacklogPublicationService publicationService,
        CommunityAccessService communityAccessService
    ) {
        this.publicationService = publicationService;
        this.communityAccessService = communityAccessService;
    }

    @PostMapping
    public BacklogPublicationService.BacklogPublicationView publish(
        @RequestBody BacklogPublicationService.PublishRequest request,
        Principal principal
    ) {
        requireModerationScope(request.communityId(), principal);
        return publicationService.publish(request, principal.getName());
    }

    /** Public: this is the claim, and a visitor has to be able to read it. */
    @GetMapping("/current")
    public BacklogPublicationService.BacklogPublicationView current(@RequestParam UUID communityId) {
        return publicationService.current(communityId).orElse(null);
    }

    /** Public: the check is worthless if only the publisher can run it. */
    @GetMapping("/verify")
    public BacklogPublicationService.Verification verify(@RequestParam UUID communityId) {
        return publicationService.verify(communityId);
    }

    @GetMapping("/history")
    public List<BacklogPublicationService.BacklogPublicationView> history(
        @RequestParam UUID communityId,
        Principal principal
    ) {
        requireModerationScope(communityId, principal);
        return publicationService.history(communityId);
    }

    private void requireModerationScope(UUID communityId, Principal principal) {
        var user = communityAccessService.getCurrentUser(principal.getName());
        communityAccessService.requireScope(
            user.getId(), communityId, CommunityPermissionScope.MANAGE_OPEN_DATA_EXPORTS);
    }
}