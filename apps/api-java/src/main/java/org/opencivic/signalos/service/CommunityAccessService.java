package org.opencivic.signalos.service;

import java.util.Set;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.exception.UnauthorizedActionException;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class CommunityAccessService {
    private final UserRepository userRepository;
    private final CommunityMembershipRepository membershipRepository;

    public CommunityAccessService(
        UserRepository userRepository,
        CommunityMembershipRepository membershipRepository
    ) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
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
                "Forbidden: community role " + membership.getRole() + " cannot perform this action."
            );
        }
        return membership;
    }
}
