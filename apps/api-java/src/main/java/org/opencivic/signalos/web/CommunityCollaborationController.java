package org.opencivic.signalos.web;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.service.CommunityCollaborationService;
import org.opencivic.signalos.web.dto.CommunityBlogPostResponse;
import org.opencivic.signalos.web.dto.CommunityFeedItemResponse;
import org.opencivic.signalos.web.dto.CommunityThreadMessageResponse;
import org.opencivic.signalos.web.dto.CommunityThreadResponse;
import org.opencivic.signalos.web.dto.CreateCommunityBlogPostRequest;
import org.opencivic.signalos.web.dto.CreateCommunityThreadMessageRequest;
import org.opencivic.signalos.web.dto.CreateCommunityThreadRequest;
import org.opencivic.signalos.web.dto.ModerateThreadMessageRequest;
import org.opencivic.signalos.web.dto.UpdateCommunityBlogPostRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.opencivic.signalos.service.CivicEngagementService;
import org.opencivic.signalos.web.dto.CivicCommentResponse;
import java.util.Map;

@RestController
@RequestMapping("/api/community")
public class CommunityCollaborationController {
    private final CommunityCollaborationService collaborationService;
    private final CivicEngagementService engagementService;

    public CommunityCollaborationController(CommunityCollaborationService collaborationService, CivicEngagementService engagementService) {
        this.collaborationService = collaborationService;
        this.engagementService = engagementService;
    }

    @GetMapping("/blog/{id}/comments")
    public List<CivicCommentResponse> getBlogComments(@PathVariable UUID id) {
        return engagementService.getComments(id, "BLOG");
    }

    @PostMapping("/blog/{id}/comments")
    public CivicCommentResponse addBlogComment(@PathVariable UUID id, @RequestBody Map<String, String> body, Principal principal) {
        return engagementService.addComment(id, "BLOG", body.get("content"), principal.getName());
    }

    @PostMapping("/blog/{id}/react")
    public Map<String, Integer> reactToBlog(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return engagementService.react(id, "BLOG", body.get("type"));
    }

    @GetMapping("/threads")
    public List<CommunityThreadResponse> getThreads(
        @RequestParam UUID communityId,
        Principal principal
    ) {
        return collaborationService.getThreads(communityId, principal.getName());
    }

    @PostMapping("/threads")
    public CommunityThreadResponse createThread(
        @Valid @RequestBody CreateCommunityThreadRequest request,
        Principal principal
    ) {
        return collaborationService.createThread(
            request.sourceCommunityId(),
            request.targetCommunityId(),
            request.relatedSignalId(),
            request.title(),
            principal.getName()
        );
    }

    @PostMapping("/threads/{threadId}/messages")
    public CommunityThreadMessageResponse addMessage(
        @PathVariable UUID threadId,
        @Valid @RequestBody CreateCommunityThreadMessageRequest request,
        Principal principal
    ) {
        return collaborationService.addMessage(
            threadId,
            request.sourceCommunityId(),
            request.content(),
            principal.getName()
        );
    }

    @PostMapping("/threads/{threadId}/messages/{messageId}/react")
    public CommunityThreadMessageResponse reactToMessage(
        @PathVariable UUID threadId,
        @PathVariable UUID messageId,
        @RequestBody java.util.Map<String, String> body,
        Principal principal
    ) {
        return collaborationService.reactToMessage(
            threadId,
            messageId,
            body.get("type"),
            principal.getName()
        );
    }

    @PatchMapping("/threads/{threadId}/messages/{messageId}/moderate")
    public CommunityThreadMessageResponse moderateMessage(
        @PathVariable UUID threadId,
        @PathVariable UUID messageId,
        @Valid @RequestBody ModerateThreadMessageRequest request,
        Principal principal
    ) {
        return collaborationService.moderateMessage(
            threadId,
            messageId,
            request.hidden(),
            request.reason(),
            principal.getName()
        );
    }

    @GetMapping("/blog")
    public List<CommunityBlogPostResponse> getBlog(
        @RequestParam UUID communityId,
        Principal principal
    ) {
        return collaborationService.getBlogTimeline(communityId, principal.getName());
    }

    @PostMapping("/blog")
    public CommunityBlogPostResponse createBlog(
        @Valid @RequestBody CreateCommunityBlogPostRequest request,
        Principal principal
    ) {
        return collaborationService.createBlogPost(
            request.communityId(),
            request.title(),
            request.content(),
            request.statusTag(),
            principal.getName()
        );
    }

    @PutMapping("/blog/{postId}")
    public CommunityBlogPostResponse updateBlog(
        @PathVariable UUID postId,
        @Valid @RequestBody UpdateCommunityBlogPostRequest request,
        Principal principal
    ) {
        return collaborationService.updateBlogPost(
            postId,
            request.title(),
            request.content(),
            request.statusTag(),
            principal.getName()
        );
    }

    @GetMapping("/feed")
    public List<CommunityFeedItemResponse> getFeed(
        @RequestParam UUID communityId,
        @RequestParam(defaultValue = "7") int days,
        Principal principal
    ) {
        return collaborationService.getCommunityFeed(communityId, days, principal.getName());
    }
}
