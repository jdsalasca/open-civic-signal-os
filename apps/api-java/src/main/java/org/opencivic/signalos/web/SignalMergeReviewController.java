package org.opencivic.signalos.web;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.service.CommunityAccessService;
import org.opencivic.signalos.service.SignalMergeReviewService;
import org.opencivic.signalos.web.dto.SignalMergeReviewRequest;
import org.opencivic.signalos.web.dto.SignalMergeReviewResponse;
import org.opencivic.signalos.web.dto.SignalMergeSuggestionResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Duplicate clustering with human approval.
 *
 * <p>Every route requires MANAGE_MODERATION_QUEUE. The algorithm proposes; a moderator decides.
 * There is deliberately no route that merges without a recorded decision, because a merge nobody
 * authorised is indistinguishable from a bug.
 */
@RestController
@RequestMapping("/api/signals/merge-review")
public class SignalMergeReviewController {

    private final SignalMergeReviewService reviewService;
    private final CommunityAccessService communityAccessService;

    public SignalMergeReviewController(
        SignalMergeReviewService reviewService,
        CommunityAccessService communityAccessService
    ) {
        this.reviewService = reviewService;
        this.communityAccessService = communityAccessService;
    }

    @GetMapping("/suggestions")
    public List<SignalMergeSuggestionResponse> getSuggestions(
        @RequestParam UUID communityId,
        @RequestParam(required = false) Double threshold,
        Principal principal
    ) {
        var user = communityAccessService.getCurrentUser(principal.getName());
        communityAccessService.requireScope(
            user.getId(), communityId, CommunityPermissionScope.MANAGE_MODERATION_QUEUE);
        return reviewService.getSuggestions(communityId, threshold, principal.getName());
    }

    @PostMapping("/decisions")
    public SignalMergeReviewResponse review(
        @RequestParam UUID communityId,
        @Valid @RequestBody SignalMergeReviewRequest request,
        Principal principal
    ) {
        var user = communityAccessService.getCurrentUser(principal.getName());
        communityAccessService.requireScope(
            user.getId(), communityId, CommunityPermissionScope.MANAGE_MODERATION_QUEUE);
        return reviewService.review(request, principal.getName());
    }

    @GetMapping("/history")
    public List<SignalMergeReviewResponse> getHistory(
        @RequestParam UUID communityId,
        Principal principal
    ) {
        var user = communityAccessService.getCurrentUser(principal.getName());
        communityAccessService.requireScope(
            user.getId(), communityId, CommunityPermissionScope.MANAGE_MODERATION_QUEUE);
        return reviewService.getHistory(communityId, principal.getName());
    }
}