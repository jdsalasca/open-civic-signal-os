package org.opencivic.signalos.web;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.opencivic.signalos.web.dto.CommunityPermissionPolicyResponse;
import org.opencivic.signalos.web.dto.CommunityBreadcrumbItemResponse;
import org.opencivic.signalos.web.dto.CreateCommunityRequest;
import org.opencivic.signalos.web.dto.CommunityMembershipResponse;
import org.opencivic.signalos.web.dto.CommunityResponse;
import org.opencivic.signalos.web.dto.CommunityTreeNodeResponse;
import org.opencivic.signalos.web.dto.JoinCommunityRequest;
import org.opencivic.signalos.web.dto.UpdateCommunityPermissionPoliciesRequest;
import org.opencivic.signalos.web.dto.UpdateCommunityRoleRequest;
import org.opencivic.signalos.service.CommunityPermissionPolicyService;
import org.opencivic.signalos.service.CommunityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/communities")
public class CommunityController {
    private final CommunityService communityService;
    private final CommunityPermissionPolicyService permissionPolicyService;

    public CommunityController(
        CommunityService communityService,
        CommunityPermissionPolicyService permissionPolicyService
    ) {
        this.communityService = communityService;
        this.permissionPolicyService = permissionPolicyService;
    }

    @GetMapping
    public List<CommunityResponse> getAllCommunities() {
        return communityService.getAllCommunities();
    }

    @GetMapping("/tree")
    public List<CommunityTreeNodeResponse> getCommunityTree() {
        return communityService.getCommunityTree();
    }

    @GetMapping("/{communityId}/breadcrumb")
    public List<CommunityBreadcrumbItemResponse> getCommunityBreadcrumb(@PathVariable UUID communityId) {
        return communityService.getCommunityBreadcrumb(communityId);
    }

    @PostMapping
    public CommunityResponse createCommunity(
        @Valid @RequestBody CreateCommunityRequest request,
        Principal principal
    ) {
        return communityService.createCommunity(
            request.name(),
            request.slug(),
            request.description(),
            request.parentCommunityId(),
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

    @GetMapping("/{communityId}/permissions")
    public List<CommunityPermissionPolicyResponse> getPermissionPolicies(
        @PathVariable UUID communityId,
        Principal principal
    ) {
        return permissionPolicyService.getPolicies(communityId, principal.getName());
    }

    @PutMapping("/{communityId}/permissions")
    public List<CommunityPermissionPolicyResponse> updatePermissionPolicies(
        @PathVariable UUID communityId,
        @Valid @RequestBody UpdateCommunityPermissionPoliciesRequest request,
        Principal principal
    ) {
        return permissionPolicyService.updatePolicies(communityId, request, principal.getName());
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
