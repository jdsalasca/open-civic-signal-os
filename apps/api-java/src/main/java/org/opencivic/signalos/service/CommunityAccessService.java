package org.opencivic.signalos.service;

import java.util.Set;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.CommunityPermissionDeniedException;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.exception.UnauthorizedActionException;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class CommunityAccessService {
    private final UserRepository userRepository;
    private final CommunityMembershipRepository membershipRepository;
    private final CommunityPermissionPolicyService permissionPolicyService;

    public CommunityAccessService(
        UserRepository userRepository,
        CommunityMembershipRepository membershipRepository,
        CommunityPermissionPolicyService permissionPolicyService
    ) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.permissionPolicyService = permissionPolicyService;
    }

    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
    }

    public CommunityMembership requireMembership(UUID userId, UUID communityId) {
        return membershipRepository.findByUserIdAndCommunityId(userId, communityId)
            .orElseThrow(() -> new UnauthorizedActionException(
                "User is not a member of community " + communityId
            ));
    }

    public CommunityMembership requireAnyRole(UUID userId, UUID communityId, Set<CommunityRole> allowedRoles) {
        CommunityMembership membership = requireMembership(userId, communityId);
        if (!allowedRoles.contains(membership.getRole())) {
            throw new org.springframework.security.access.AccessDeniedException(
                "User role is not allowed for this action in community " + communityId
            );
        }
        return membership;
    }

    public CommunityMembership requireScope(UUID userId, UUID communityId, CommunityPermissionScope scope) {
        CommunityMembership membership = requireMembership(userId, communityId);
        Set<CommunityRole> allowedRoles = permissionPolicyService.resolveAllowedRoles(communityId, scope);
        if (!allowedRoles.contains(membership.getRole())) {
            throw new CommunityPermissionDeniedException(
                communityId,
                scope,
                membership.getRole(),
                allowedRoles
            );
        }
        return membership;
    }
}
