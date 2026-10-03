package org.opencivic.signalos.web;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityDigestPublication;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.service.CommunityAccessService;
import org.opencivic.signalos.service.DigestSchedulerService;
import org.opencivic.signalos.service.WeeklyDigestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Weekly civic digest generation.
 *
 * <p>Generating and publishing are separate routes so a digest can be read before it reaches
 * anyone. Publishing is idempotent per week: a second attempt is refused, because a weekly bulletin
 * delivered twice is the kind of failure that costs trust exactly when the platform is asking for it.
 *
 * <p>Delivery to a channel is not wired here. The email connector exists; handing it a digest body
 * is the next slice and needs a decision about who is accountable when one goes out wrong.
 */
@RestController
@RequestMapping("/api/community/weekly-digest")
public class WeeklyDigestController {

    private final WeeklyDigestService digestService;
    private final DigestSchedulerService schedulerService;
    private final CommunityAccessService communityAccessService;

    public WeeklyDigestController(
        WeeklyDigestService digestService,
        DigestSchedulerService schedulerService,
        CommunityAccessService communityAccessService
    ) {
        this.digestService = digestService;
        this.schedulerService = schedulerService;
        this.communityAccessService = communityAccessService;
    }

    @GetMapping
    public WeeklyDigestService.WeeklyDigest getDigest(
        @RequestParam UUID communityId,
        @RequestParam(required = false) String week,
        @RequestParam(required = false) Integer limit,
        Principal principal
    ) {
        requireExportScope(communityId, principal);
        return digestService.buildDigest(communityId, week, limit, principal.getName());
    }

    @PostMapping("/publish")
    public WeeklyDigestService.WeeklyDigest publish(
        @RequestParam UUID communityId,
        @RequestParam(required = false) String week,
        @RequestParam(required = false) Integer limit,
        Principal principal
    ) {
        requireExportScope(communityId, principal);
        return digestService.publishDigest(communityId, week, limit, principal.getName());
    }

    @GetMapping("/history")
    public List<CommunityDigestPublication> history(
        @RequestParam UUID communityId,
        Principal principal
    ) {
        requireExportScope(communityId, principal);
        return digestService.history(communityId, principal.getName());
    }

    /**
     * What the scheduler did, per week.
     *
     * <p>Readable so a community can see that a digest is waiting for someone to publish it, rather
     * than discovering on Friday that no bulletin went out.
     */
    @GetMapping("/schedule-history")
    public List<DigestSchedulerService.ScheduleRunView> scheduleHistory(
        @RequestParam UUID communityId,
        Principal principal
    ) {
        requireExportScope(communityId, principal);
        return schedulerService.history(communityId);
    }

    private void requireExportScope(UUID communityId, Principal principal) {
        var user = communityAccessService.getCurrentUser(principal.getName());
        communityAccessService.requireScope(
            user.getId(), communityId, CommunityPermissionScope.MANAGE_OPEN_DATA_EXPORTS);
    }
}