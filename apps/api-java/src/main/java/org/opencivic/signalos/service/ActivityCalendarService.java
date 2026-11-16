package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityActivity;
import org.opencivic.signalos.domain.CommunityActivitySignup;
import org.opencivic.signalos.domain.CommunityActivitySignupStatus;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityActivityRepository;
import org.opencivic.signalos.repository.CommunityActivitySignupRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityCalendarService {
    private static final DateTimeFormatter ICS_DATE = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private final CommunityActivityRepository activityRepository;
    private final CommunityActivitySignupRepository signupRepository;
    private final UserRepository userRepository;
    private final CommunityAccessService accessService;
    private final UserNotificationService notificationService;

    public ActivityCalendarService(
        CommunityActivityRepository activityRepository,
        CommunityActivitySignupRepository signupRepository,
        UserRepository userRepository,
        CommunityAccessService accessService,
        UserNotificationService notificationService
    ) {
        this.activityRepository = activityRepository;
        this.signupRepository = signupRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public String calendar(UUID communityId, UUID activityId, String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        accessService.requireMembership(user.getId(), communityId);
        CommunityActivity activity = requireActivity(communityId, activityId);
        return ics(activity);
    }

    /**
     * Sends one in-app reminder per confirmed volunteer, roughly 24 hours before the activity.
     * The marker lives on the activity so an hourly run cannot notify twice.
     */
    @Scheduled(cron = "${app.activity-reminders.cron:0 0 * * * *}")
    @Transactional
    public void sendDueReminders() {
        sendDueReminders(LocalDateTime.now());
    }

    @Transactional
    public int sendDueReminders(LocalDateTime now) {
        List<CommunityActivity> due = activityRepository
            .findByCancelledFalseAndReminderSentAtIsNullAndStartsAtBetween(
                now.plusHours(23),
                now.plusHours(25)
            );
        int sent = 0;
        for (CommunityActivity activity : due) {
            for (CommunityActivitySignup signup : signupRepository.findByActivityIdOrderByCreatedAtAsc(activity.getId())) {
                if (signup.getStatus() != CommunityActivitySignupStatus.CONFIRMED) {
                    continue;
                }
                notificationService.notifyUser(
                    signup.getVolunteerId(),
                    activity.getOrganizerId(),
                    null,
                    "ACTIVITY_REMINDER",
                    "ACTIVITY",
                    activity.getId(),
                    activity.getTitle(),
                    activity.getCommunityId()
                );
                sent++;
            }
            activity.setReminderSentAt(now);
            activityRepository.save(activity);
        }
        return sent;
    }

    private CommunityActivity requireActivity(UUID communityId, UUID activityId) {
        CommunityActivity activity = activityRepository.findById(activityId)
            .orElseThrow(() -> new ResourceNotFoundException("Activity not found: " + activityId));
        if (!communityId.equals(activity.getCommunityId())) {
            throw new ResourceNotFoundException("Activity not found: " + activityId);
        }
        return activity;
    }

    private String ics(CommunityActivity activity) {
        String description = activity.getDescription() == null ? "" : activity.getDescription();
        return String.join("\r\n",
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "PRODID:-//Open Civic Signal OS//Community Activities//EN",
            "CALSCALE:GREGORIAN",
            "BEGIN:VEVENT",
            "UID:" + activity.getId() + "@open-civic-signal-os",
            "DTSTAMP:" + ICS_DATE.format(LocalDateTime.now()),
            "DTSTART:" + ICS_DATE.format(activity.getStartsAt()),
            "DTEND:" + ICS_DATE.format(activity.getEndsAt()),
            "SUMMARY:" + escape(activity.getTitle()),
            "DESCRIPTION:" + escape(description),
            "LOCATION:" + escape(activity.getLocationLabel()),
            "END:VEVENT",
            "END:VCALENDAR",
            ""
        );
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace(",", "\\,")
            .replace(";", "\\;");
    }
}
