package org.opencivic.signalos.service;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityMembershipAudit;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityMembershipAuditRepository;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.web.dto.CommunityBreadcrumbItemResponse;
import org.opencivic.signalos.web.dto.CommunityMembershipResponse;
import org.opencivic.signalos.web.dto.CommunityResponse;
import org.opencivic.signalos.web.dto.CommunityTreeNodeResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityService {
    private final CommunityRepository communityRepository;
    private final CommunityMembershipRepository membershipRepository;
    private final CommunityMembershipAuditRepository membershipAuditRepository;
    private final UserRepository userRepository;
    private final CommunityAccessService accessService;

    public CommunityService(
        CommunityRepository communityRepository,
        CommunityMembershipRepository membershipRepository,
        CommunityMembershipAuditRepository membershipAuditRepository,
        UserRepository userRepository,
        CommunityAccessService accessService
    ) {
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.membershipAuditRepository = membershipAuditRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
    }

    public List<CommunityMembershipResponse> getMyMemberships(String username) {
        User user = accessService.getCurrentUser(username);
        Map<UUID, Community> communitiesById = communityRepository.findAll().stream()
            .collect(Collectors.toMap(Community::getId, Function.identity()));
        return membershipRepository.findByUserId(user.getId()).stream()
            .map(membership -> toResponse(membership, communitiesById))
            .toList();
    }

    public List<CommunityResponse> getAllCommunities() {
        return communityRepository.findAll().stream()
            .map(this::toCommunityResponse)
            .toList();
    }

    public List<CommunityTreeNodeResponse> getCommunityTree() {
        List<Community> communities = communityRepository.findAll();
        return buildTree(communities, null);
    }

    public List<CommunityBreadcrumbItemResponse> getCommunityBreadcrumb(UUID communityId) {
        Map<UUID, Community> communitiesById = communityRepository.findAll().stream()
            .collect(Collectors.toMap(Community::getId, Function.identity()));
        Community community = communitiesById.get(communityId);
        if (community == null) {
            throw new ResourceNotFoundException("Community not found: " + communityId);
        }
        return breadcrumbFor(community, communitiesById);
    }

    @Transactional
    public CommunityResponse createCommunity(
        String name,
        String slug,
        String description,
        UUID parentCommunityId,
        String username
    ) {
        User user = accessService.getCurrentUser(username);
        if (parentCommunityId != null) {
            communityRepository.findById(parentCommunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent community not found: " + parentCommunityId));
        }
        Community community = new Community();
        community.setName(name);
        community.setSlug(slug);
        community.setDescription(description);
        community.setParentCommunityId(parentCommunityId);
        Community savedCommunity = communityRepository.save(community);

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(savedCommunity.getId());
        membership.setUserId(user.getId());
        membership.setRole(CommunityRole.COORDINATOR);
        membership.setCreatedBy(user.getId());
        membershipRepository.save(membership);
        saveAudit(savedCommunity.getId(), user.getId(), user.getId(), null, CommunityRole.COORDINATOR);
        return toCommunityResponse(savedCommunity);
    }

    @Transactional
    public CommunityMembershipResponse joinCommunity(UUID communityId, CommunityRole role, String username) {
        User user = accessService.getCurrentUser(username);
        communityRepository.findById(communityId)
            .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));
        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(user.getId());
        membership.setRole(role);
        membership.setCreatedBy(user.getId());
        try {
            CommunityMembership saved = membershipRepository.save(membership);
            saveAudit(communityId, user.getId(), user.getId(), null, role);
            return toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            CommunityMembership existing = membershipRepository
                .findByUserIdAndCommunityId(user.getId(), communityId)
                .orElseThrow(() -> ex);
            return toResponse(existing);
        }
    }

    @Transactional
    public CommunityMembershipResponse updateRole(
        UUID communityId,
        UUID targetUserId,
        CommunityRole role,
        String username
    ) {
        User actor = accessService.getCurrentUser(username);
        accessService.requireAnyRole(
            actor.getId(),
            communityId,
            java.util.Set.of(CommunityRole.COORDINATOR)
        );
        userRepository.findById(targetUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Target user not found: " + targetUserId));
        CommunityMembership membership = membershipRepository
            .findByUserIdAndCommunityId(targetUserId, communityId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Membership not found for user " + targetUserId + " in community " + communityId
            ));
        CommunityRole previousRole = membership.getRole();
        membership.setRole(role);
        CommunityMembership saved = membershipRepository.save(membership);
        saveAudit(communityId, targetUserId, actor.getId(), previousRole, role);
        return toResponse(saved);
    }

    private void saveAudit(
        UUID communityId,
        UUID targetUserId,
        UUID changedBy,
        CommunityRole previousRole,
        CommunityRole newRole
    ) {
        CommunityMembershipAudit audit = new CommunityMembershipAudit();
        audit.setCommunityId(communityId);
        audit.setTargetUserId(targetUserId);
        audit.setChangedBy(changedBy);
        audit.setPreviousRole(previousRole);
        audit.setNewRole(newRole);
        membershipAuditRepository.save(audit);
    }

    private CommunityMembershipResponse toResponse(CommunityMembership membership) {
        Map<UUID, Community> communitiesById = communityRepository.findAll().stream()
            .collect(Collectors.toMap(Community::getId, Function.identity()));
        return toResponse(membership, communitiesById);
    }

    private CommunityMembershipResponse toResponse(
        CommunityMembership membership,
        Map<UUID, Community> communitiesById
    ) {
        Community community = communitiesById.get(membership.getCommunityId());
        if (community == null) {
            throw new ResourceNotFoundException(
                "Community not found for membership: " + membership.getCommunityId()
            );
        }
        return new CommunityMembershipResponse(
            membership.getUserId(),
            membership.getCommunityId(),
            community.getName(),
            community.getSlug(),
            community.getParentCommunityId(),
            breadcrumbFor(community, communitiesById),
            membership.getRole().name(),
            membership.getCreatedBy(),
            membership.getCreatedAt()
        );
    }

    private CommunityResponse toCommunityResponse(Community community) {
        return new CommunityResponse(
            community.getId(),
            community.getName(),
            community.getSlug(),
            community.getDescription(),
            community.getParentCommunityId(),
            community.getCreatedAt()
        );
    }

    private List<CommunityTreeNodeResponse> buildTree(List<Community> communities, UUID parentCommunityId) {
        return communities.stream()
            .filter(community -> {
                if (parentCommunityId == null) {
                    return community.getParentCommunityId() == null;
                }
                return parentCommunityId.equals(community.getParentCommunityId());
            })
            .sorted(Comparator.comparing(Community::getName, String.CASE_INSENSITIVE_ORDER))
            .map(community -> new CommunityTreeNodeResponse(
                community.getId(),
                community.getName(),
                community.getSlug(),
                community.getDescription(),
                community.getParentCommunityId(),
                buildTree(communities, community.getId())
            ))
            .toList();
    }

    private List<CommunityBreadcrumbItemResponse> breadcrumbFor(
        Community community,
        Map<UUID, Community> communitiesById
    ) {
        LinkedList<CommunityBreadcrumbItemResponse> breadcrumb = new LinkedList<>();
        Community current = community;
        while (current != null) {
            breadcrumb.addFirst(new CommunityBreadcrumbItemResponse(
                current.getId(),
                current.getName(),
                current.getSlug()
            ));
            UUID parentId = current.getParentCommunityId();
            current = parentId == null ? null : communitiesById.get(parentId);
        }
        return breadcrumb;
    }
}
