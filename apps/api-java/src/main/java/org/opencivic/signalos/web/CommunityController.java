package org.opencivic.signalos.web;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.web.dto.CreateCommunityRequest;
import org.opencivic.signalos.web.dto.CommunityMembershipResponse;
import org.opencivic.signalos.web.dto.JoinCommunityRequest;
import org.opencivic.signalos.web.dto.UpdateCommunityRoleRequest;
import org.opencivic.signalos.service.CommunityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/communities")
public class CommunityController {
    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping
    public List<Community> getAllCommunities() {
        return communityService.getAllCommunities();
    }

    @PostMapping
    public Community createCommunity(
        @Valid @RequestBody CreateCommunityRequest request,
        Principal principal
    ) {
        return communityService.createCommunity(
            request.name(),
            request.slug(),
            request.description(),
            principal.getName()
        );
    }

    @GetMapping("/my")
    public List<CommunityMembershipResponse> myMemberships(Principal principal) {
        return communityService.getMyMemberships(principal.getName());
    }

    @PostMapping("/{communityId}/join")
    public CommunityMembershipResponse joinCommunity(
        @PathVariable UUID communityId,
        @Valid @RequestBody JoinCommunityRequest request,
        Principal principal
    ) {
        return communityService.joinCommunity(communityId, request.role(), principal.getName());
    }

    @PatchMapping("/{communityId}/memberships/{userId}/role")
    public CommunityMembershipResponse updateRole(
        @PathVariable UUID communityId,
        @PathVariable UUID userId,
        @Valid @RequestBody UpdateCommunityRoleRequest request,
        Principal principal
    ) {
        return communityService.updateRole(communityId, userId, request.role(), principal.getName());
    }

    @PostMapping("/{communityId}/switch")
    public ResponseEntity<Map<String, Object>> switchContext(@PathVariable UUID communityId, Principal principal) {
        List<CommunityMembershipResponse> memberships = communityService.getMyMemberships(principal.getName());
        boolean exists = memberships.stream().anyMatch(m -> m.communityId().equals(communityId));
        if (!exists) {
            throw new org.springframework.security.access.AccessDeniedException(
                "Cannot activate a community context without membership."
            );
        }
        return ResponseEntity.ok(Map.of("communityId", communityId, "status", "active"));
    }
}
