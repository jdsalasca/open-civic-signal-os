package org.opencivic.signalos.service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityMembershipAudit;
import org.opencivic.signalos.domain.CommunityPermissionScope;
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
        Map<UUID, Long> memberCounts = memberCountsByCommunity();
        return communityRepository.findAll().stream()
            .map(community -> toCommunityResponse(community, memberCounts.getOrDefault(community.getId(), 0L)))
            .toList();
    }

    /**
     * The public directory behind the landing page: every city, largest first.
     *
     * <p>Readable without a session on purpose. A person deciding whether registering is worth it
     * needs to see the communities that already exist, not be told to sign up first. Sorted by size so
     * the places with a community already forming are the ones a newcomer sees first.
     */
    public List<CommunityResponse> getPublicCities() {
        Map<UUID, Long> memberCounts = memberCountsByCommunity();
        return communityRepository.findAll().stream()
            .map(community -> toCommunityResponse(community, memberCounts.getOrDefault(community.getId(), 0L)))
            .sorted(Comparator
                .comparingLong(CommunityResponse::memberCount).reversed()
                .thenComparing(CommunityResponse::name, String.CASE_INSENSITIVE_ORDER))
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
        String country,
        String region,
        UUID parentCommunityId,
        String username
    ) {
        User user = accessService.getCurrentUser(username);
        return createCommunityAndJoin(user.getId(), name, slug, description, country, region, parentCommunityId,
            CommunityRole.COORDINATOR);
    }

    /**
     * Founds a city for someone who has no session yet, which is what registration needs.
     *
     * <p>Separate from {@link #createCommunity} because that one resolves the founder from the
     * authenticated principal, and at registration there is no principal: the person exists but has
     * not verified or logged in. The registrant becomes COORDINATOR, the role the platform treats as
     * the organizer accountable for a community.
     */
    @Transactional
    public CommunityResponse createCommunityAndJoin(
        UUID userId,
        String name,
        String slug,
        String description,
        String country,
        String region,
        UUID parentCommunityId,
        CommunityRole role
    ) {
        if (parentCommunityId != null) {
            communityRepository.findById(parentCommunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent community not found: " + parentCommunityId));
        }
        Community community = new Community();
        community.setName(name);
        community.setSlug(resolveSlug(slug, name));
        community.setDescription(description);
        community.setCountry(trimToNull(country));
        community.setRegion(trimToNull(region));
        community.setParentCommunityId(parentCommunityId);
        Community savedCommunity = communityRepository.save(community);

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(savedCommunity.getId());
        membership.setUserId(userId);
        membership.setRole(role);
        membership.setCreatedBy(userId);
        membershipRepository.save(membership);
        saveAudit(savedCommunity.getId(), userId, userId, null, role);
        return toCommunityResponse(savedCommunity, 1L);
    }

    public Community requireCommunity(UUID communityId) {
        return communityRepository.findById(communityId)
            .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));
    }

    @Transactional
    public CommunityMembershipResponse joinCommunity(UUID communityId, CommunityRole role, String username) {
        User user = accessService.getCurrentUser(username);
        return joinCommunityForUser(communityId, user.getId(), role);
    }

    /**
     * Adds a membership for a user id, used by registration before a principal exists.
     *
     * <p>Idempotent: joining a city twice returns the existing membership instead of failing, so a
     * retried registration or a double tap does not read as an error.
     */
    @Transactional
    public CommunityMembershipResponse joinCommunityForUser(UUID communityId, UUID userId, CommunityRole role) {
        requireCommunity(communityId);
        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(userId);
        membership.setRole(role);
        membership.setCreatedBy(userId);
        try {
            CommunityMembership saved = membershipRepository.save(membership);
            saveAudit(communityId, userId, userId, null, role);
            return toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            CommunityMembership existing = membershipRepository
                .findByUserIdAndCommunityId(userId, communityId)
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
        accessService.requireScope(actor.getId(), communityId, CommunityPermissionScope.MANAGE_MEMBERSHIPS);
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

    private Map<UUID, Long> memberCountsByCommunity() {
        return membershipRepository.countGroupedByCommunity().stream()
            .collect(Collectors.toMap(
                row -> (UUID) row[0],
                row -> (Long) row[1]
            ));
    }

    /**
     * A slug for a city founded from its name alone.
     *
     * <p>The registration form asks for a name, not a slug, so one is derived: lowercased, accents
     * folded, non-alphanumerics collapsed to single dashes. Uniqueness is enforced by suffixing,
     * because two towns can share a name and a unique constraint should not turn that into a failed
     * registration.
     */
    private String resolveSlug(String requestedSlug, String name) {
        String base = slugify(requestedSlug == null || requestedSlug.isBlank() ? name : requestedSlug);
        if (base.isBlank()) {
            base = "community";
        }
        String candidate = base;
        int suffix = 2;
        while (communityRepository.findBySlug(candidate).isPresent()) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private String slugify(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    private CommunityResponse toCommunityResponse(Community community, long memberCount) {
        return new CommunityResponse(
            community.getId(),
            community.getName(),
            community.getSlug(),
            community.getDescription(),
            community.getCountry(),
            community.getRegion(),
            community.getParentCommunityId(),
            memberCount,
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
