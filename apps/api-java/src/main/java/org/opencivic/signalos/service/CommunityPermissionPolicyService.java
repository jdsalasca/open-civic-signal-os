package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityPermissionPolicy;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.CommunityPermissionDeniedException;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityPermissionPolicyRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.web.dto.CommunityPermissionPolicyResponse;
import org.opencivic.signalos.web.dto.CommunityPermissionPolicyRuleRequest;
import org.opencivic.signalos.web.dto.UpdateCommunityPermissionPoliciesRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityPermissionPolicyService {
    private final CommunityPermissionPolicyRepository policyRepository;
    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final CommunityMembershipRepository membershipRepository;

    public CommunityPermissionPolicyService(
        CommunityPermissionPolicyRepository policyRepository,
        CommunityRepository communityRepository,
        UserRepository userRepository,
        CommunityMembershipRepository membershipRepository
    ) {
        this.policyRepository = policyRepository;
        this.communityRepository = communityRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    public List<CommunityPermissionPolicyResponse> getPolicies(UUID communityId, String username) {
        User user = getCurrentUser(username);
        requireMembership(user.getId(), communityId);
        return resolvePolicies(communityId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public List<CommunityPermissionPolicyResponse> updatePolicies(
        UUID communityId,
        UpdateCommunityPermissionPoliciesRequest request,
        String username
    ) {
        User user = getCurrentUser(username);
        CommunityMembership membership = requireMembership(user.getId(), communityId);
        if (membership.getRole() != CommunityRole.COORDINATOR) {
            throw new CommunityPermissionDeniedException(
                communityId,
                CommunityPermissionScope.MANAGE_PERMISSION_POLICIES,
                membership.getRole(),
                CommunityPermissionScope.MANAGE_PERMISSION_POLICIES.defaultAllowedRoles()
            );
        }
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));

        Map<CommunityPermissionScope, CommunityPermissionPolicy> existingPolicies = policyRepository
            .findByCommunityIdOrderByScopeAsc(communityId)
            .stream()
            .collect(Collectors.toMap(CommunityPermissionPolicy::getScope, Function.identity()));

        for (CommunityPermissionPolicyRuleRequest rule : request.policies()) {
            CommunityPermissionScope scope = CommunityPermissionScope.valueOf(rule.scope().trim().toUpperCase());
            Set<CommunityRole> roles = parseRoles(rule.allowedRoles());
            if (roles.isEmpty()) {
                throw new IllegalArgumentException("At least one role must be allowed for scope " + scope.name());
            }
            CommunityPermissionPolicy policy = existingPolicies.getOrDefault(scope, new CommunityPermissionPolicy());
            policy.setCommunityId(community.getId());
            policy.setScope(scope);
            policy.setAllowedRoles(roles.stream().map(Enum::name).sorted().collect(Collectors.joining(",")));
            policy.setUpdatedBy(user.getId());
            policy.setUpdatedAt(LocalDateTime.now());
            policyRepository.save(policy);
        }

        return getPolicies(communityId, username);
    }

    public Set<CommunityRole> resolveAllowedRoles(UUID communityId, CommunityPermissionScope scope) {
        return policyRepository.findByCommunityIdAndScope(communityId, scope)
            .map(CommunityPermissionPolicy::getAllowedRoles)
            .map(this::parseRoles)
            .orElse(scope.defaultAllowedRoles());
    }

    private List<CommunityPermissionPolicy> resolvePolicies(UUID communityId) {
        Map<CommunityPermissionScope, CommunityPermissionPolicy> stored = policyRepository
            .findByCommunityIdOrderByScopeAsc(communityId)
            .stream()
            .collect(Collectors.toMap(CommunityPermissionPolicy::getScope, Function.identity()));

        return Arrays.stream(CommunityPermissionScope.values())
            .sorted(Comparator.comparing(Enum::name))
            .map(scope -> stored.getOrDefault(scope, defaultPolicy(communityId, scope)))
            .toList();
    }

    private CommunityPermissionPolicy defaultPolicy(UUID communityId, CommunityPermissionScope scope) {
        CommunityPermissionPolicy policy = new CommunityPermissionPolicy();
        policy.setCommunityId(communityId);
        policy.setScope(scope);
        policy.setAllowedRoles(scope.defaultAllowedRoles().stream()
            .map(Enum::name)
            .sorted()
            .collect(Collectors.joining(",")));
        return policy;
    }

    private CommunityPermissionPolicyResponse toResponse(CommunityPermissionPolicy policy) {
        return new CommunityPermissionPolicyResponse(
            policy.getCommunityId(),
            policy.getScope().name(),
            parseRoles(policy.getAllowedRoles()).stream().map(Enum::name).sorted().toList(),
            policy.getUpdatedBy(),
            policy.getUpdatedAt()
        );
    }

    private Set<CommunityRole> parseRoles(List<String> roles) {
        return roles.stream()
            .map(role -> CommunityRole.valueOf(role.trim().toUpperCase()))
            .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private Set<CommunityRole> parseRoles(String roles) {
        if (roles == null || roles.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(roles.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .map(value -> CommunityRole.valueOf(value.toUpperCase()))
            .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
    }

    private CommunityMembership requireMembership(UUID userId, UUID communityId) {
        return membershipRepository.findByUserIdAndCommunityId(userId, communityId)
            .orElseThrow(() -> new org.opencivic.signalos.exception.UnauthorizedActionException(
                "User is not a member of community " + communityId
            ));
    }
}
