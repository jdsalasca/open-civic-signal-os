package org.opencivic.signalos.service;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityMembershipAudit;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ConflictException;
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
import org.springframework.scheduling.annotation.Scheduled;
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
            // A neighbourhood that emptied out is inactive; city entries stay listed so a new
            // person can still open one.
            .filter(community -> community.isActive()
                || (community.getLocality() == null && community.getNeighborhood() == null))
            .map(community -> toCommunityResponse(community, memberCounts.getOrDefault(community.getId(), 0L)))
            .sorted(Comparator
                .comparingLong(CommunityResponse::memberCount).reversed()
                .thenComparing(CommunityResponse::name, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    /** The closest active city entry, used by the "use my location" onboarding step. */
    public Optional<CommunityResponse> nearestCity(double latitude, double longitude) {
        Map<UUID, Long> memberCounts = memberCountsByCommunity();
        return communityRepository.findAll().stream()
            .filter(Community::isActive)
            .filter(c -> c.getCity() != null && c.getLocality() == null && c.getNeighborhood() == null)
            .filter(c -> c.getLatitude() != null && c.getLongitude() != null)
            .min(Comparator.comparingDouble(c -> distanceSquared(c, latitude, longitude)))
            .map(c -> toCommunityResponse(c, memberCounts.getOrDefault(c.getId(), 0L)));
    }

    private double distanceSquared(Community community, double latitude, double longitude) {
        double deltaLat = community.getLatitude() - latitude;
        double deltaLon = (community.getLongitude() - longitude) * Math.cos(Math.toRadians(latitude));
        return deltaLat * deltaLat + deltaLon * deltaLon;
    }

    /**
     * A locality or neighbourhood with no members is inactive. Runs at startup and hourly; city
     * entries are the catalog and are never deactivated.
     */
    @Scheduled(fixedDelayString = "${app.communities.lifecycle-sweep-ms:3600000}")
    @Transactional
    public void deactivateEmptyPlaces() {
        Map<UUID, Long> memberCounts = memberCountsByCommunity();
        for (Community community : communityRepository.findAll()) {
            boolean isCity = community.getLocality() == null && community.getNeighborhood() == null;
            if (isCity) {
                continue;
            }
            boolean shouldBeActive = memberCounts.getOrDefault(community.getId(), 0L) > 0;
            if (community.isActive() != shouldBeActive) {
                community.setActive(shouldBeActive);
                communityRepository.save(community);
            }
        }
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
        return createCommunity(
            name, slug, description, country, region, parentCommunityId,
            null, null, null, username
        );
    }

    public CommunityResponse createCommunity(
        String name,
        String slug,
        String description,
        String country,
        String region,
        UUID parentCommunityId,
        String city,
        String locality,
        String neighborhood,
        String username
    ) {
        User user = accessService.getCurrentUser(username);
        return createCommunityAndJoin(
            user.getId(), name, slug, description, country, region, parentCommunityId,
            city, locality, neighborhood, CommunityRole.COORDINATOR
        );
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
        return createCommunityAndJoin(
            userId, name, slug, description, country, region, parentCommunityId,
            null, null, null, role
        );
    }

    /**
     * Creates a community from a place: city, locality and neighborhood are normalized here, in the
     * trust boundary, so the UI never decides what a duplicate is.
     *
     * <p>The visible name is the most specific level provided. The place key (city|locality|
     * neighborhood, accent- and case-insensitive) makes a second community for the same place a
     * 409 instead of a directory full of near-duplicates.
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
        String city,
        String locality,
        String neighborhood,
        CommunityRole role
    ) {
        String normalizedCity = PlaceNames.normalize(city);
        String normalizedLocality = PlaceNames.normalize(locality);
        String normalizedNeighborhood = PlaceNames.normalize(neighborhood);
        if (normalizedCity == null && (normalizedLocality != null || normalizedNeighborhood != null)) {
            throw new IllegalArgumentException(
                "City is required when a locality or neighborhood is provided."
            );
        }
        String placeKey = PlaceNames.key(normalizedCity, normalizedLocality, normalizedNeighborhood);
        if (placeKey != null) {
            communityRepository.findByPlaceKey(placeKey).ifPresent(existing -> {
                throw new ConflictException(
                    "A community already exists for this place: " + existing.getName()
                        + ". Join it from the directory instead of creating a duplicate."
                );
            });
        }
        String resolvedName = normalizedNeighborhood != null
            ? normalizedNeighborhood
            : normalizedLocality != null
                ? normalizedLocality
                : normalizedCity != null ? normalizedCity : trimToNull(name);
        if (resolvedName == null) {
            throw new IllegalArgumentException("Community name or city is required.");
        }

        // A locality or barrio hangs from its city in the tree. The city is created once if the
        // catalog does not have it yet, without a member: it is a directory entry, not a second
        // community the registrant belongs to.
        if (parentCommunityId == null && normalizedCity != null
            && (normalizedLocality != null || normalizedNeighborhood != null)) {
            parentCommunityId = ensureCity(normalizedCity, country, region).getId();
        }
        UUID resolvedParentId = parentCommunityId;
        if (resolvedParentId != null) {
            communityRepository.findById(resolvedParentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent community not found: " + resolvedParentId));
        }
        Community community = new Community();
        community.setName(resolvedName);
        community.setSlug(resolveSlug(slug, resolvedName));
        community.setDescription(description);
        community.setCountry(trimToNull(country));
        community.setRegion(trimToNull(region));
        community.setCity(normalizedCity);
        community.setLocality(normalizedLocality);
        community.setNeighborhood(normalizedNeighborhood);
        community.setPlaceKey(placeKey);
        community.setParentCommunityId(resolvedParentId);
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
        Community community = requireCommunity(communityId);
        if (!community.isActive()) {
            community.setActive(true);
            communityRepository.save(community);
        }
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

    /** The city entry for a barrio's tree; created once, without a member. */
    private Community ensureCity(String city, String country, String region) {
        String placeKey = PlaceNames.key(city, null, null);
        return communityRepository.findByPlaceKey(placeKey).orElseGet(() -> {
            Community community = new Community();
            community.setName(city);
            community.setSlug(resolveSlug(null, city));
            community.setCountry(trimToNull(country));
            community.setRegion(trimToNull(region));
            community.setCity(city);
            community.setPlaceKey(placeKey);
            community.setDescription("Espacio cívico de " + city + ".");
            return communityRepository.save(community);
        });
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
        return PlaceNames.slug(value);
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
            community.getCity(),
            community.getLocality(),
            community.getNeighborhood(),
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
