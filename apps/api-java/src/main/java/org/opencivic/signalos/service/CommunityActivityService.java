package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityActivity;
import org.opencivic.signalos.domain.CommunityActivityAttendanceStatus;
import org.opencivic.signalos.domain.CommunityActivitySignup;
import org.opencivic.signalos.domain.CommunityActivitySignupStatus;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ConflictException;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityActivityRepository;
import org.opencivic.signalos.repository.CommunityActivitySignupRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.web.dto.CommunityActivityBoardResponse;
import org.opencivic.signalos.web.dto.CommunityActivityResponse;
import org.opencivic.signalos.web.dto.CommunityActivitySignupResponse;
import org.opencivic.signalos.web.dto.CommunityActivityVolunteerResponse;
import org.opencivic.signalos.web.dto.CreateCommunityActivityRequest;
import org.opencivic.signalos.web.dto.MarkCommunityActivityAttendanceRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityActivityService {
    static final String WINDOW_NOT_OPEN = "Signups for this activity are not open yet.";
    static final String WINDOW_CLOSED = "Signups for this activity closed at the published deadline.";
    static final String ACTIVITY_FULL = "This activity already reached its published capacity.";

    private final CommunityAccessService communityAccessService;
    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final CommunityActivityRepository activityRepository;
    private final CommunityActivitySignupRepository signupRepository;

    public CommunityActivityService(
        CommunityAccessService communityAccessService,
        CommunityRepository communityRepository,
        UserRepository userRepository,
        CommunityActivityRepository activityRepository,
        CommunityActivitySignupRepository signupRepository
    ) {
        this.communityAccessService = communityAccessService;
        this.communityRepository = communityRepository;
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
        this.signupRepository = signupRepository;
    }

    @Transactional(readOnly = true)
    public CommunityActivityBoardResponse getBoard(UUID communityId, String username, Integer limit) {
        User user = communityAccessService.getCurrentUser(username);
        communityAccessService.requireMembership(user.getId(), communityId);
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));
        int max = CommunityListLimits.resolveLimit(limit);

        List<CommunityActivityResponse> activities = activityRepository
            .findByCommunityIdAndCancelledFalseOrderByStartsAtAsc(communityId).stream()
            .limit(max)
            .map(activity -> toResponse(activity, user.getId()))
            .toList();

        int myUpcomingSignups = (int) signupRepository
            .findByVolunteerIdAndStatusOrderByCreatedAtDesc(user.getId(), CommunityActivitySignupStatus.CONFIRMED).stream()
            .filter(signup -> activities.stream().anyMatch(a -> a.id().equals(signup.getActivityId())))
            .count();

        return new CommunityActivityBoardResponse(
            community.getId(),
            community.getName(),
            activities.size(),
            myUpcomingSignups,
            activities
        );
    }

    @Transactional
    public CommunityActivityBoardResponse createActivity(CreateCommunityActivityRequest request, String username) {
        User user = communityAccessService.getCurrentUser(username);
        if (request.communityId() == null) {
            throw new IllegalArgumentException("communityId is required to create an activity.");
        }
        communityAccessService.requireScope(user.getId(), request.communityId(), CommunityPermissionScope.MANAGE_ACTIVITIES);

        validateSchedule(request);

        CommunityActivity activity = new CommunityActivity();
        activity.setCommunityId(request.communityId());
        activity.setTitle(request.title().trim());
        activity.setDescription(request.description().trim());
        activity.setLocationLabel(request.locationLabel().trim());
        activity.setStartsAt(request.startsAt());
        activity.setEndsAt(request.endsAt());
        activity.setSignupOpensAt(request.signupOpensAt());
        activity.setSignupClosesAt(request.signupClosesAt());
        activity.setSignupCapacity(request.signupCapacity());
        activity.setOrganizerId(user.getId());
        activityRepository.save(activity);

        return getBoard(request.communityId(), username, null);
    }

    @Transactional
    public CommunityActivitySignupResponse join(UUID communityId, UUID activityId, String username) {
        User user = communityAccessService.getCurrentUser(username);
        communityAccessService.requireScope(user.getId(), communityId, CommunityPermissionScope.JOIN_ACTIVITIES);
        CommunityActivity activity = requireActivity(communityId, activityId);

        if (activity.isCancelled()) {
            throw new ConflictException("This activity was cancelled by its organizer.");
        }
        LocalDateTime now = LocalDateTime.now();
        enforceWindow(activity, now);

        long confirmed = signupRepository.countByActivityIdAndStatus(activityId, CommunityActivitySignupStatus.CONFIRMED);
        if (confirmed >= activity.getSignupCapacity()) {
            throw new ConflictException(ACTIVITY_FULL);
        }

        CommunityActivitySignup signup = signupRepository.findByActivityIdAndVolunteerId(activityId, user.getId()).orElse(null);
        if (signup != null && signup.isConfirmed()) {
            throw new ConflictException("You already hold a confirmed spot for this activity.");
        }
        if (signup == null) {
            signup = new CommunityActivitySignup();
            signup.setActivityId(activityId);
            signup.setCommunityId(communityId);
            signup.setVolunteerId(user.getId());
            signup.setCreatedAt(now);
        }
        signup.setStatus(CommunityActivitySignupStatus.CONFIRMED);
        signup.setCancelledAt(null);
        signup.setAttendanceStatus(CommunityActivityAttendanceStatus.PENDING);
        signup.setAttendedAt(null);
        signupRepository.save(signup);

        return new CommunityActivitySignupResponse(
            activityId, communityId, signup.getId(), signup.getStatus().name(), "signup_confirmed"
        );
    }

    @Transactional
    public CommunityActivitySignupResponse leave(UUID communityId, UUID activityId, String username) {
        User user = communityAccessService.getCurrentUser(username);
        communityAccessService.requireScope(user.getId(), communityId, CommunityPermissionScope.JOIN_ACTIVITIES);
        CommunityActivity activity = requireActivity(communityId, activityId);

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(activity.getSignupClosesAt())) {
            throw new ConflictException(WINDOW_CLOSED + " Contact the organizer directly from now on.");
        }

        CommunityActivitySignup signup = signupRepository.findByActivityIdAndVolunteerId(activityId, user.getId())
            .orElseThrow(() -> new ConflictException("You are not signed up for this activity."));
        if (!signup.isConfirmed()) {
            throw new ConflictException("You already released your spot for this activity.");
        }

        signup.setStatus(CommunityActivitySignupStatus.CANCELLED);
        signup.setCancelledAt(now);
        signupRepository.save(signup);

        return new CommunityActivitySignupResponse(
            activityId, communityId, signup.getId(), signup.getStatus().name(), "signup_released"
        );
    }

    @Transactional
    public CommunityActivitySignupResponse recordAttendance(
        MarkCommunityActivityAttendanceRequest request,
        String username
    ) {
        User user = communityAccessService.getCurrentUser(username);
        communityAccessService.requireScope(
            user.getId(), request.communityId(), CommunityPermissionScope.MANAGE_ACTIVITIES
        );
        requireActivity(request.communityId(), request.activityId());

        CommunityActivitySignup signup = signupRepository.findById(request.signupId())
            .orElseThrow(() -> new ResourceNotFoundException("Signup not found: " + request.signupId()));
        if (!signup.getActivityId().equals(request.activityId())) {
            throw new ResourceNotFoundException("Signup not found: " + request.signupId());
        }

        if (isBlank(request.attendanceStatus())) {
            throw new IllegalArgumentException("attendanceStatus must be one of PENDING, ATTENDED, NO_SHOW.");
        }
        CommunityActivityAttendanceStatus attendance = CommunityActivityAttendanceStatus
            .valueOf(request.attendanceStatus().trim().toUpperCase());
        signup.setAttendanceStatus(attendance);
        signup.setAttendedAt(attendance == CommunityActivityAttendanceStatus.PENDING ? null : LocalDateTime.now());
        signupRepository.save(signup);

        return new CommunityActivitySignupResponse(
            request.activityId(), request.communityId(), signup.getId(), signup.getStatus().name(),
            "attendance_" + attendance.name().toLowerCase()
        );
    }

    @Transactional
    public CommunityActivitySignupResponse cancelActivity(UUID communityId, UUID activityId, String username) {
        User user = communityAccessService.getCurrentUser(username);
        communityAccessService.requireScope(user.getId(), communityId, CommunityPermissionScope.MANAGE_ACTIVITIES);
        CommunityActivity activity = requireActivity(communityId, activityId);

        activity.setCancelled(true);
        activityRepository.save(activity);

        return new CommunityActivitySignupResponse(
            activityId, communityId, null, CommunityActivitySignupStatus.CANCELLED.name(), "activity_cancelled"
        );
    }

    private void enforceWindow(CommunityActivity activity, LocalDateTime now) {
        if (now.isBefore(activity.getSignupOpensAt())) {
            throw new ConflictException(WINDOW_NOT_OPEN);
        }
        if (now.isAfter(activity.getSignupClosesAt())) {
            throw new ConflictException(WINDOW_CLOSED);
        }
    }

    private void validateSchedule(CreateCommunityActivityRequest request) {
        if (isBlank(request.title()) || request.title().trim().length() < 3) {
            throw new IllegalArgumentException("Activity title must be at least 3 characters.");
        }
        if (isBlank(request.description()) || isBlank(request.locationLabel())) {
            throw new IllegalArgumentException("Activity description and location are required.");
        }
        if (request.startsAt() == null || request.endsAt() == null
            || request.signupOpensAt() == null || request.signupClosesAt() == null) {
            throw new IllegalArgumentException("Activity start, end, and signup window timestamps are required.");
        }
        if (request.signupCapacity() == null || request.signupCapacity() < 1) {
            throw new IllegalArgumentException("Signup capacity must be at least 1.");
        }
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new IllegalArgumentException("Activity end time must be after its start time.");
        }
        if (!request.signupOpensAt().isBefore(request.signupClosesAt())) {
            throw new IllegalArgumentException("Signup window must open before it closes.");
        }
        if (request.signupClosesAt().isAfter(request.startsAt())) {
            throw new IllegalArgumentException("Signup window must close no later than the activity start time.");
        }
    }

    private CommunityActivityResponse toResponse(CommunityActivity activity, UUID viewerId) {
        LocalDateTime now = LocalDateTime.now();
        List<CommunityActivitySignup> signups = signupRepository.findByActivityIdOrderByCreatedAtAsc(activity.getId());
        long confirmed = signups.stream().filter(CommunityActivitySignup::isConfirmed).count();

        CommunityActivitySignup mine = signups.stream()
            .filter(signup -> signup.getVolunteerId().equals(viewerId))
            .findFirst()
            .orElse(null);

        String windowState = resolveWindowState(activity, now, confirmed);
        long fillRate = activity.getSignupCapacity() == 0
            ? 0
            : Math.min(100, (confirmed * 100) / activity.getSignupCapacity());

        return new CommunityActivityResponse(
            activity.getId(),
            activity.getCommunityId(),
            activity.getTitle(),
            activity.getDescription(),
            activity.getLocationLabel(),
            activity.getStartsAt(),
            activity.getEndsAt(),
            activity.getSignupOpensAt(),
            activity.getSignupClosesAt(),
            activity.getSignupCapacity(),
            activity.getOrganizerId(),
            displayNameOf(activity.getOrganizerId()),
            activity.getCreatedAt(),
            activity.isCancelled(),
            windowState,
            "OPEN".equals(windowState),
            resolveFullReason(windowState),
            confirmed,
            fillRate,
            mine == null ? null : mine.getId(),
            mine == null ? null : mine.getStatus().name(),
            signups.stream().map(this::toVolunteerResponse).toList()
        );
    }

    static String resolveWindowState(CommunityActivity activity, LocalDateTime now, long confirmedCount) {
        if (activity.isCancelled()) {
            return "CANCELLED";
        }
        if (now.isBefore(activity.getSignupOpensAt())) {
            return "UPCOMING";
        }
        if (now.isAfter(activity.getSignupClosesAt())) {
            return "CLOSED";
        }
        if (confirmedCount >= activity.getSignupCapacity()) {
            return "FULL";
        }
        return "OPEN";
    }

    private static String resolveFullReason(String windowState) {
        return switch (windowState) {
            case "UPCOMING" -> WINDOW_NOT_OPEN;
            case "CLOSED", "CANCELLED" -> WINDOW_CLOSED;
            case "FULL" -> ACTIVITY_FULL;
            default -> null;
        };
    }

    private CommunityActivityVolunteerResponse toVolunteerResponse(CommunityActivitySignup signup) {
        return new CommunityActivityVolunteerResponse(
            signup.getId(),
            signup.getVolunteerId(),
            displayNameOf(signup.getVolunteerId()),
            signup.getStatus().name(),
            signup.getAttendanceStatus().name(),
            signup.getCreatedAt(),
            signup.getCancelledAt(),
            signup.getAttendedAt()
        );
    }

    private String displayNameOf(UUID userId) {
        return userRepository.findById(userId)
            .map(user -> user.getDisplayName() != null && !user.getDisplayName().isBlank()
                ? user.getDisplayName()
                : user.getUsername())
            .orElse("unknown");
    }

    private CommunityActivity requireActivity(UUID communityId, UUID activityId) {
        CommunityActivity activity = activityRepository.findById(activityId)
            .orElseThrow(() -> new ResourceNotFoundException("Activity not found: " + activityId));
        if (!activity.getCommunityId().equals(communityId)) {
            throw new ResourceNotFoundException("Activity not found: " + activityId);
        }
        return activity;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}