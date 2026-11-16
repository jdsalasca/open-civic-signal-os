package org.opencivic.signalos.web;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import org.opencivic.signalos.service.ContentFollowService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/follows")
public class ContentFollowController {
    private final ContentFollowService followService;

    public ContentFollowController(ContentFollowService followService) {
        this.followService = followService;
    }

    @GetMapping("/{targetType}/{targetId}")
    public Map<String, Boolean> status(
        @PathVariable String targetType,
        @PathVariable UUID targetId,
        Principal principal
    ) {
        return Map.of("following", followService.isFollowing(targetType, targetId, principal.getName()));
    }

    @PostMapping("/{targetType}/{targetId}")
    public Map<String, Boolean> follow(
        @PathVariable String targetType,
        @PathVariable UUID targetId,
        Principal principal
    ) {
        followService.follow(targetType, targetId, principal.getName());
        return Map.of("following", true);
    }

    @DeleteMapping("/{targetType}/{targetId}")
    public Map<String, Boolean> unfollow(
        @PathVariable String targetType,
        @PathVariable UUID targetId,
        Principal principal
    ) {
        followService.unfollow(targetType, targetId, principal.getName());
        return Map.of("following", false);
    }
}
