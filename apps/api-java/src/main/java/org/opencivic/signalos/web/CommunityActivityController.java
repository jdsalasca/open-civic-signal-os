package org.opencivic.signalos.web;

import java.security.Principal;
import java.util.UUID;
import org.opencivic.signalos.service.CommunityActivityService;
import org.opencivic.signalos.web.dto.CommunityActivityBoardResponse;
import org.opencivic.signalos.web.dto.CommunityActivitySignupResponse;
import org.opencivic.signalos.web.dto.CreateCommunityActivityRequest;
import org.opencivic.signalos.web.dto.MarkCommunityActivityAttendanceRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community/activities")
public class CommunityActivityController {
    private final CommunityActivityService activityService;

    public CommunityActivityController(CommunityActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/board")
    public CommunityActivityBoardResponse getBoard(
        @RequestParam UUID communityId,
        Principal principal
    ) {
        return activityService.getBoard(communityId, principal.getName());
    }

    @PostMapping
    public CommunityActivityBoardResponse createActivity(
        @RequestBody CreateCommunityActivityRequest request,
        Principal principal
    ) {
        return activityService.createActivity(request, principal.getName());
    }

    @PostMapping("/{activityId}/signups")
    public CommunityActivitySignupResponse join(
        @PathVariable UUID activityId,
        @RequestParam UUID communityId,
        Principal principal
    ) {
        return activityService.join(communityId, activityId, principal.getName());
    }

    @DeleteMapping("/{activityId}/signups")
    public CommunityActivitySignupResponse leave(
        @PathVariable UUID activityId,
        @RequestParam UUID communityId,
        Principal principal
    ) {
        return activityService.leave(communityId, activityId, principal.getName());
    }

    @PatchMapping("/{activityId}/attendance")
    public CommunityActivitySignupResponse recordAttendance(
        @PathVariable UUID activityId,
        @RequestBody MarkCommunityActivityAttendanceRequest request,
        Principal principal
    ) {
        return activityService.recordAttendance(
            new MarkCommunityActivityAttendanceRequest(
                request.communityId(),
                activityId,
                request.signupId(),
                request.attendanceStatus()
            ),
            principal.getName()
        );
    }

    @DeleteMapping("/{activityId}")
    public CommunityActivitySignupResponse cancelActivity(
        @PathVariable UUID activityId,
        @RequestParam UUID communityId,
        Principal principal
    ) {
        return activityService.cancelActivity(communityId, activityId, principal.getName());
    }
}