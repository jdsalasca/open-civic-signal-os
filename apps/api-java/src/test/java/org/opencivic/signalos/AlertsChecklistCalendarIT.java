package org.opencivic.signalos;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityActivity;
import org.opencivic.signalos.domain.CommunityActivitySignup;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityActivityRepository;
import org.opencivic.signalos.repository.CommunityActivitySignupRepository;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserNotificationRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.service.ActivityCalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:alertschecklistitdb;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AlertsChecklistCalendarIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private CommunityMembershipRepository membershipRepository;

    @Autowired
    private SignalRepository signalRepository;

    @Autowired
    private CommunityActivityRepository activityRepository;

    @Autowired
    private CommunityActivitySignupRepository signupRepository;

    @Autowired
    private UserNotificationRepository notificationRepository;

    @Autowired
    private ActivityCalendarService calendarService;

    private UUID userId;
    private UUID volunteerId;
    private UUID communityId;
    private UUID signalId;
    private UUID activityId;

    @BeforeEach
    void setUp() {
        User user = new User("alerts_user", "{noop}pw", "alerts@test.dev", "ROLE_CITIZEN");
        user.setEnabled(true);
        user.setVerified(true);
        userId = userRepository.save(user).getId();

        User volunteer = new User("alerts_volunteer", "{noop}pw", "alerts-volunteer@test.dev", "ROLE_CITIZEN");
        volunteer.setEnabled(true);
        volunteer.setVerified(true);
        volunteerId = userRepository.save(volunteer).getId();

        Community community = new Community();
        community.setName("Alert District");
        community.setSlug("alert-district");
        community.setDescription("Alerts test");
        communityId = communityRepository.save(community).getId();

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(userId);
        membership.setRole(CommunityRole.MEMBER);
        membership.setCreatedBy(userId);
        membershipRepository.save(membership);

        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setTitle("A new report");
        signal.setDescription("For the checklist");
        signal.setCategory("INFRASTRUCTURE");
        signal.setStatus("NEW");
        signal.setCommunityId(communityId);
        signal.setAuthorId(userId);
        signal.setCreatedAt(LocalDateTime.now());
        signal.setPriorityScore(10.0);
        signalId = signalRepository.save(signal).getId();

        LocalDateTime startsAt = LocalDateTime.now().plusHours(24);
        CommunityActivity activity = new CommunityActivity();
        activity.setCommunityId(communityId);
        activity.setTitle("Community cleanup");
        activity.setDescription("Bring gloves, bags and water.");
        activity.setLocationLabel("Central park");
        activity.setStartsAt(startsAt);
        activity.setEndsAt(startsAt.plusHours(2));
        activity.setSignupOpensAt(startsAt.minusDays(3));
        activity.setSignupClosesAt(startsAt.minusHours(2));
        activity.setSignupCapacity(20);
        activity.setOrganizerId(userId);
        activityId = activityRepository.save(activity).getId();

        CommunityActivitySignup signup = new CommunityActivitySignup();
        signup.setActivityId(activityId);
        signup.setCommunityId(communityId);
        signup.setVolunteerId(volunteerId);
        signupRepository.save(signup);
    }

    @Test
    @WithMockUser(username = "alerts_user", roles = {"CITIZEN"})
    void alertsCanBeSubscribedAndRemoved() throws Exception {
        mockMvc.perform(post("/api/communities/{id}/alerts", communityId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subscribed").value(true));
        mockMvc.perform(get("/api/communities/{id}/alerts", communityId))
            .andExpect(jsonPath("$.subscribed").value(true));
        mockMvc.perform(delete("/api/communities/{id}/alerts", communityId))
            .andExpect(jsonPath("$.subscribed").value(false));
    }

    @Test
    @WithMockUser(username = "alerts_user", roles = {"CITIZEN"})
    void onboardingChecklistCompletesAsTheUserActs() throws Exception {
        mockMvc.perform(get("/api/onboarding/checklist"))
            .andExpect(jsonPath("$.joinedCommunity").value(true))
            .andExpect(jsonPath("$.contributed").value(true))
            .andExpect(jsonPath("$.followingContent").value(false))
            .andExpect(jsonPath("$.allComplete").value(false));

        mockMvc.perform(post("/api/follows/SIGNAL/{id}", signalId))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/onboarding/checklist"))
            .andExpect(jsonPath("$.followingContent").value(true))
            .andExpect(jsonPath("$.allComplete").value(true));
    }

    @Test
    @WithMockUser(username = "alerts_user", roles = {"CITIZEN"})
    void activityExportsCalendarAndSendsOneReminder() throws Exception {
        mockMvc.perform(get("/api/community/activities/{id}/calendar", activityId)
                .param("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/calendar"))
            .andExpect(content().string(containsString("SUMMARY:Community cleanup")));

        int firstRun = calendarService.sendDueReminders(LocalDateTime.now());
        int secondRun = calendarService.sendDueReminders(LocalDateTime.now());

        org.assertj.core.api.Assertions.assertThat(firstRun).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(secondRun).isZero();
        org.assertj.core.api.Assertions.assertThat(
            notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(volunteerId)
        ).anyMatch(notification -> "ACTIVITY_REMINDER".equals(notification.getType())
            && activityId.equals(notification.getTargetId()));
    }
}
