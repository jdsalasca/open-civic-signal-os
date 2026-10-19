package org.opencivic.signalos.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.domain.CommunityResource;
import org.opencivic.signalos.domain.CommunityResourceBooking;
import org.opencivic.signalos.domain.CommunityResourceBookingStatus;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ConflictException;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.CommunityResourceBookingRepository;
import org.opencivic.signalos.repository.CommunityResourceRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.web.dto.CommunityResourceBoardResponse;
import org.opencivic.signalos.web.dto.CommunityResourceBookingResponse;
import org.opencivic.signalos.web.dto.CommunityResourceResponse;
import org.opencivic.signalos.web.dto.CreateCommunityResourceBookingRequest;
import org.opencivic.signalos.web.dto.CreateCommunityResourceRequest;
import org.opencivic.signalos.web.dto.DecideCommunityResourceBookingRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityResourceService {
    static final List<CommunityResourceBookingStatus> BLOCKING_STATUSES = List.of(
        CommunityResourceBookingStatus.PENDING_APPROVAL,
        CommunityResourceBookingStatus.APPROVED
    );

    private final CommunityAccessService communityAccessService;
    private final CommunityPermissionPolicyService permissionPolicyService;
    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final CommunityResourceRepository resourceRepository;
    private final CommunityResourceBookingRepository bookingRepository;

    public CommunityResourceService(
        CommunityAccessService communityAccessService,
        CommunityPermissionPolicyService permissionPolicyService,
        CommunityRepository communityRepository,
        UserRepository userRepository,
        CommunityResourceRepository resourceRepository,
        CommunityResourceBookingRepository bookingRepository
    ) {
        this.communityAccessService = communityAccessService;
        this.permissionPolicyService = permissionPolicyService;
        this.communityRepository = communityRepository;
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public CommunityResourceBoardResponse getBoard(UUID communityId, String username, Integer limit) {
        User user = communityAccessService.getCurrentUser(username);
        CommunityMembership membership = communityAccessService.requireMembership(user.getId(), communityId);
        boolean canManage = canManage(membership, communityId);
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));
        int max = CommunityListLimits.resolveLimit(limit);

        LocalDateTime now = LocalDateTime.now();
        List<CommunityResourceBookingResponse> allBookings = bookingRepository
            .findByCommunityIdOrderByRequestedAtDesc(communityId).stream()
            .map(booking -> toBookingResponse(booking, loadResourceName(booking.getResourceId())))
            .toList();

        List<CommunityResourceResponse> resources = resourceRepository
            .findByCommunityIdAndArchivedFalseOrderByCreatedAtDesc(communityId).stream()
            .limit(max)
            .map(resource -> toResourceResponse(resource, user.getId(), allBookings, now))
            .toList();

        List<CommunityResourceBookingResponse> approvals = canManage
            ? allBookings.stream()
                .filter(booking -> CommunityResourceBookingStatus.PENDING_APPROVAL.name().equals(booking.status()))
                .toList()
            : List.of();

        return new CommunityResourceBoardResponse(
            community.getId(),
            community.getName(),
            resources.size(),
            approvals.size(),
            resources,
            allBookings.stream().filter(booking -> booking.requesterId().equals(user.getId())).toList(),
            approvals
        );
    }

    @Transactional
    public CommunityResourceBoardResponse createResource(CreateCommunityResourceRequest request, String username) {
        User user = communityAccessService.getCurrentUser(username);
        if (request.communityId() == null) {
            throw new IllegalArgumentException("communityId is required to create a resource.");
        }
        communityAccessService.requireScope(user.getId(), request.communityId(), CommunityPermissionScope.MANAGE_RESOURCES);

        if (isBlank(request.name()) || request.name().trim().length() < 3) {
            throw new IllegalArgumentException("Resource name must be at least 3 characters.");
        }
        if (isBlank(request.description()) || isBlank(request.locationLabel())) {
            throw new IllegalArgumentException("Resource description and location are required.");
        }
        int maxBookingHours = request.maxBookingHours() == null ? 8 : request.maxBookingHours();
        if (maxBookingHours < 1) {
            throw new IllegalArgumentException("maxBookingHours must be at least 1.");
        }
        int minNoticeHours = request.minNoticeHours() == null ? 0 : request.minNoticeHours();
        if (minNoticeHours < 0) {
            throw new IllegalArgumentException("minNoticeHours cannot be negative.");
        }

        CommunityResource resource = new CommunityResource();
        resource.setCommunityId(request.communityId());
        resource.setName(request.name().trim());
        resource.setDescription(request.description().trim());
        resource.setLocationLabel(request.locationLabel().trim());
        resource.setRequiresApproval(Boolean.TRUE.equals(request.requiresApproval()));
        resource.setMinNoticeHours(minNoticeHours);
        resource.setMaxBookingHours(maxBookingHours);
        resource.setCreatedBy(user.getId());
        resourceRepository.save(resource);

        return getBoard(request.communityId(), username, null);
    }

    @Transactional
    public CommunityResourceBookingResponse requestBooking(
        CreateCommunityResourceBookingRequest request,
        String username
    ) {
        User user = communityAccessService.getCurrentUser(username);
        communityAccessService.requireScope(user.getId(), request.communityId(), CommunityPermissionScope.BOOK_RESOURCES);
        CommunityResource resource = requireResource(request.communityId(), request.resourceId());

        if (resource.isArchived()) {
            throw new ConflictException("This resource is archived and cannot be booked.");
        }
        if (isBlank(request.purpose())) {
            throw new IllegalArgumentException("Explain what the resource is needed for.");
        }
        if (request.startsAt() == null || request.endsAt() == null) {
            throw new IllegalArgumentException("Booking start and end timestamps are required.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new IllegalArgumentException("Booking end time must be after its start time.");
        }
        if (!request.startsAt().isAfter(now)) {
            throw new IllegalArgumentException("Bookings must start in the future.");
        }

        long minutes = Duration.between(request.startsAt(), request.endsAt()).toMinutes();
        if (minutes > resource.getMaxBookingHours() * 60L) {
            throw new ConflictException(
                "This resource can be booked for at most %d hours.".formatted(resource.getMaxBookingHours())
            );
        }
        if (resource.getMinNoticeHours() > 0 && request.startsAt().isBefore(now.plusHours(resource.getMinNoticeHours()))) {
            throw new ConflictException(
                "This resource requires at least %d hours notice.".formatted(resource.getMinNoticeHours())
            );
        }

        List<CommunityResourceBooking> conflicts = bookingRepository.findOverlapping(
            resource.getId(), request.startsAt(), request.endsAt(), BLOCKING_STATUSES
        );
        if (!conflicts.isEmpty()) {
            throw new ConflictException(
                "This window is already booked. Choose another slot or ask the organizer to escalate the conflict."
            );
        }

        CommunityResourceBooking booking = new CommunityResourceBooking();
        booking.setResourceId(resource.getId());
        booking.setCommunityId(request.communityId());
        booking.setRequesterId(user.getId());
        booking.setPurpose(request.purpose().trim());
        booking.setStartsAt(request.startsAt());
        booking.setEndsAt(request.endsAt());
        booking.setRequestedAt(now);
        booking.setStatus(resource.isRequiresApproval()
            ? CommunityResourceBookingStatus.PENDING_APPROVAL
            : CommunityResourceBookingStatus.APPROVED);
        booking.setDecidedAt(resource.isRequiresApproval() ? null : now);
        booking.setDecidedBy(resource.isRequiresApproval() ? null : user.getId());
        booking = bookingRepository.save(booking);

        return toBookingResponse(booking, resource.getName());
    }

    @Transactional
    public CommunityResourceBookingResponse decideBooking(
        DecideCommunityResourceBookingRequest request,
        String username
    ) {
        User user = communityAccessService.getCurrentUser(username);
        if (request.communityId() == null) {
            throw new IllegalArgumentException("communityId is required to decide a booking.");
        }
        communityAccessService.requireScope(user.getId(), request.communityId(), CommunityPermissionScope.MANAGE_RESOURCES);

        CommunityResourceBooking booking = requireBooking(request.communityId(), request.bookingId());
        if (booking.getStatus() != CommunityResourceBookingStatus.PENDING_APPROVAL) {
            throw new ConflictException("This booking was already decided.");
        }

        // A pending booking already blocked the window, so approving cannot introduce a
        // new conflict that the request-time check did not already see.
        boolean approve = !Boolean.FALSE.equals(request.approve());
        booking.setStatus(approve ? CommunityResourceBookingStatus.APPROVED : CommunityResourceBookingStatus.REJECTED);
        booking.setDecisionNote(isBlank(request.decisionNote()) ? null : request.decisionNote().trim());
        booking.setDecidedAt(LocalDateTime.now());
        booking.setDecidedBy(user.getId());
        bookingRepository.save(booking);

        return toBookingResponse(booking, loadResourceName(booking.getResourceId()));
    }

    @Transactional
    public CommunityResourceBookingResponse cancelBooking(UUID communityId, UUID bookingId, String username) {
        User user = communityAccessService.getCurrentUser(username);
        CommunityMembership membership = communityAccessService.requireMembership(user.getId(), communityId);
        CommunityResourceBooking booking = requireBooking(communityId, bookingId);

        boolean isRequester = booking.getRequesterId().equals(user.getId());
        if (!isRequester && !canManage(membership, communityId)) {
            throw new AccessDeniedException("Only the requester or a resource manager can cancel this booking.");
        }
        if (booking.getStatus() == CommunityResourceBookingStatus.CANCELLED) {
            throw new ConflictException("This booking was already cancelled.");
        }

        booking.setStatus(CommunityResourceBookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);

        return toBookingResponse(booking, loadResourceName(booking.getResourceId()));
    }

    @Transactional
    public CommunityResourceBoardResponse archiveResource(UUID communityId, UUID resourceId, String username) {
        User user = communityAccessService.getCurrentUser(username);
        communityAccessService.requireScope(user.getId(), communityId, CommunityPermissionScope.MANAGE_RESOURCES);
        CommunityResource resource = requireResource(communityId, resourceId);

        resource.setArchived(true);
        resourceRepository.save(resource);

        return getBoard(communityId, username, null);
    }

    private boolean canManage(CommunityMembership membership, UUID communityId) {
        return permissionPolicyService
            .resolveAllowedRoles(communityId, CommunityPermissionScope.MANAGE_RESOURCES)
            .contains(membership.getRole());
    }

    private CommunityResourceBooking requireBooking(UUID communityId, UUID bookingId) {
        CommunityResourceBooking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        if (!booking.getCommunityId().equals(communityId)) {
            throw new ResourceNotFoundException("Booking not found: " + bookingId);
        }
        return booking;
    }

    private CommunityResource requireResource(UUID communityId, UUID resourceId) {
        CommunityResource resource = resourceRepository.findById(resourceId)
            .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));
        if (!resource.getCommunityId().equals(communityId)) {
            throw new ResourceNotFoundException("Resource not found: " + resourceId);
        }
        return resource;
    }

    private CommunityResourceResponse toResourceResponse(
        CommunityResource resource,
        UUID viewerId,
        List<CommunityResourceBookingResponse> allBookings,
        LocalDateTime now
    ) {
        List<CommunityResourceBookingResponse> upcoming = allBookings.stream()
            .filter(booking -> booking.resourceId().equals(resource.getId()))
            .filter(booking -> booking.endsAt().isAfter(now))
            .filter(booking -> BLOCKING_STATUSES.stream().anyMatch(s -> s.name().equals(booking.status())))
            .toList();

        CommunityResourceBookingResponse mine = upcoming.stream()
            .filter(booking -> booking.requesterId().equals(viewerId))
            .findFirst()
            .orElse(null);

        return new CommunityResourceResponse(
            resource.getId(),
            resource.getCommunityId(),
            resource.getName(),
            resource.getDescription(),
            resource.getLocationLabel(),
            resource.isRequiresApproval(),
            resource.getMinNoticeHours(),
            resource.getMaxBookingHours(),
            resource.isArchived(),
            resource.getCreatedBy(),
            resource.getCreatedAt(),
            upcoming.size(),
            mine == null ? null : mine.id(),
            mine == null ? null : mine.status(),
            upcoming
        );
    }

    private CommunityResourceBookingResponse toBookingResponse(CommunityResourceBooking booking, String resourceName) {
        return new CommunityResourceBookingResponse(
            booking.getId(),
            booking.getResourceId(),
            resourceName,
            booking.getCommunityId(),
            booking.getRequesterId(),
            displayNameOf(booking.getRequesterId()),
            booking.getPurpose(),
            booking.getStartsAt(),
            booking.getEndsAt(),
            booking.getStatus().name(),
            booking.getDecisionNote(),
            booking.getRequestedAt(),
            booking.getDecidedAt(),
            booking.getDecidedBy() == null ? null : displayNameOf(booking.getDecidedBy()),
            booking.getCancelledAt()
        );
    }

    private String loadResourceName(UUID resourceId) {
        return resourceRepository.findById(resourceId)
            .map(CommunityResource::getName)
            .orElse("unknown");
    }

    private String displayNameOf(UUID userId) {
        return userRepository.findById(userId)
            .map(user -> user.getDisplayName() != null && !user.getDisplayName().isBlank()
                ? user.getDisplayName()
                : user.getUsername())
            .orElse("unknown");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}