package org.opencivic.signalos.service;

import java.util.Set;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.CommunitySanction;
import org.opencivic.signalos.domain.CommunitySanctionStatus;
import org.opencivic.signalos.domain.CommunitySanctionType;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.CommunityPermissionDeniedException;
import org.opencivic.signalos.exception.CommunitySanctionActiveException;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.exception.UnauthorizedActionException;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunitySanctionRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class CommunityAccessService {
    private final UserRepository userRepository;
    private final CommunityMembershipRepository membershipRepository;
    private final CommunityPermissionPolicyService permissionPolicyService;
    private final CommunitySanctionRepository sanctionRepository;

    public CommunityAccessService(
        UserRepository userRepository,
        CommunityMembershipRepository membershipRepository,
        CommunityPermissionPolicyService permissionPolicyService,
        CommunitySanctionRepository sanctionRepository
    ) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.permissionPolicyService = permissionPolicyService;
        this.sanctionRepository = sanctionRepository;
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
        enforceActiveSanction(userId, communityId, scope);
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

    private void enforceActiveSanction(UUID userId, UUID communityId, CommunityPermissionScope scope) {
        if (scope == CommunityPermissionScope.MANAGE_MODERATION_QUEUE || scope == CommunityPermissionScope.VIEW_SENSITIVE_DATA) {
            return;
        }
        CommunitySanction sanction = sanctionRepository.findActiveSanctions(
            communityId,
            userId,
            CommunitySanctionStatus.ACTIVE,
            java.time.LocalDateTime.now()
        ).stream().findFirst().orElse(null);
        if (sanction == null || !blocksScope(sanction.getSanctionType())) {
            return;
        }
        throw new CommunitySanctionActiveException(
            communityId,
            sanction.getSanctionType(),
            sanction.getEndsAt(),
            buildSanctionMessage(sanction)
        );
    }

    private boolean blocksScope(CommunitySanctionType sanctionType) {
        return sanctionType != CommunitySanctionType.WARN;
    }

    private String buildSanctionMessage(CommunitySanction sanction) {
        String untilText = sanction.getEndsAt() == null ? "until a moderator reviews the sanction" : "until " + sanction.getEndsAt();
        return switch (sanction.getSanctionType()) {
            case LIMIT_POSTING_7_DAYS -> "Community posting privileges are limited " + untilText + ".";
            case SUSPEND_7_DAYS, SUSPEND_30_DAYS -> "Community participation is suspended " + untilText + ".";
            case WARN -> "A moderation warning is active on this community account.";
        };
    }
}
