package org.opencivic.signalos;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:activitiesit;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommunityActivityIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;

    private UUID communityId;
    private UUID organizerId;

    @BeforeEach
    void setUp() {
        organizerId = saveUser("act_coord", "Activity Coordinator");
        saveUser("act_member", "Activity Member");

        Community community = new Community();
        community.setName("Activities District");
        community.setSlug("activities-district");
        community.setDescription("Volunteering slots");
        communityId = communityRepository.save(community).getId();

        addMembership(organizerId, CommunityRole.COORDINATOR);
        addMembership(userRepository.findByUsername("act_member").orElseThrow().getId(), CommunityRole.MEMBER);
    }

    private UUID saveUser(String username, String displayName) {
        User user = new User(username, "encoded", username + "@example.com", "ROLE_CITIZEN");
        user.setDisplayName(displayName);
        user.setVerified(true);
        user.setEnabled(true);
        return userRepository.save(user).getId();
    }

    private void addMembership(UUID userId, CommunityRole role) {
        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(userId);
        membership.setRole(role);
        membership.setCreatedBy(organizerId);
        membershipRepository.save(membership);
    }

    @Test
    void organizerShouldCreateActivityAndMemberShouldJoinWithinWindow() throws Exception {
        String activityId = createActivity("act_coord", "Lighting audit walk", 2, 4);

        mockMvc.perform(post("/api/community/activities/{activityId}/signups", activityId)
                .with(user("act_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.message").value("signup_confirmed"));

        mockMvc.perform(get("/api/community/activities/board")
                .with(user("act_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activities", hasSize(1)))
            .andExpect(jsonPath("$.activities[0].confirmedCount").value(1))
            .andExpect(jsonPath("$.activities[0].fillRatePercent").value(50))
            .andExpect(jsonPath("$.activities[0].roster", hasSize(1)))
            .andExpect(jsonPath("$.activities[0].roster[0].volunteerName").value("Activity Member"))
            .andExpect(jsonPath("$.myUpcomingSignups").value(1));
    }

    @Test
    void memberShouldNotBeAbleToCreateActivity() throws Exception {
        createActivity("act_member", "Unauthorized activity", 5, 4, 403);
    }

    @Test
    void capacityShouldBeEnforced() throws Exception {
        String activityId = createActivity("act_coord", "Bridge inspection", 1, 4);
        saveUser("act_extra", "Extra Volunteer");
        addMembership(userRepository.findByUsername("act_extra").orElseThrow().getId(), CommunityRole.MEMBER);

        mockMvc.perform(post("/api/community/activities/{activityId}/signups", activityId)
                .with(user("act_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/community/activities/{activityId}/signups", activityId)
                .with(user("act_extra").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isConflict());

        mockMvc.perform(get("/api/community/activities/board")
                .with(user("act_extra").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activities[0].signupWindowState").value("FULL"))
            .andExpect(jsonPath("$.activities[0].signupOpen").value(false))
            .andExpect(jsonPath("$.activities[0].fullReason").value("This activity already reached its published capacity."));
    }

    @Test
    void signupWindowShouldBlockEarlyJoinAndLateRelease() throws Exception {
        String closedActivityId = createActivity("act_coord", "Already closed", 3, -1);
        mockMvc.perform(post("/api/community/activities/{activityId}/signups", closedActivityId)
                .with(user("act_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isConflict());

        String openActivityId = createActivity("act_coord", "Open window", 3, 4);
        mockMvc.perform(post("/api/community/activities/{activityId}/signups", openActivityId)
                .with(user("act_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/api/community/activities/{activityId}/signups", openActivityId)
                .with(user("act_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(delete("/api/community/activities/{activityId}/signups", openActivityId)
                .with(user("act_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isConflict());
    }

    @Test
    void duplicateSignupShouldBeRejected() throws Exception {
        String activityId = createActivity("act_coord", "Neighbourhood clean-up", 5, 4);

        mockMvc.perform(post("/api/community/activities/{activityId}/signups", activityId)
                .with(user("act_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/community/activities/{activityId}/signups", activityId)
                .with(user("act_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isConflict());
    }

    @Test
    void organizerShouldRecordAttendanceButMemberShouldNot() throws Exception {
        String activityId = createActivity("act_coord", "Compost training", 5, 4);

        MvcResult joinResult = mockMvc.perform(post("/api/community/activities/{activityId}/signups", activityId)
                .with(user("act_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andReturn();

        String signupId = new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(joinResult.getResponse().getContentAsString()).get("signupId").asText();

        mockMvc.perform(patch("/api/community/activities/{activityId}/attendance", activityId)
                .with(user("act_member").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "signupId": "%s",
                      "attendanceStatus": "ATTENDED"
                    }
                    """.formatted(communityId, signupId)))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/community/activities/{activityId}/attendance", activityId)
                .with(user("act_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "signupId": "%s",
                      "attendanceStatus": "ATTENDED"
                    }
                    """.formatted(communityId, signupId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("attendance_attended"));

        mockMvc.perform(get("/api/community/activities/board")
                .with(user("act_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activities[0].roster[0].attendanceStatus").value("ATTENDED"));
    }

    @Test
    void invalidScheduleShouldBeRejected() throws Exception {
        LocalDateTime startsAt = LocalDateTime.now().plusHours(4);
        // Signup window closes after the activity starts: must be rejected.
        String body = """
            {
              "communityId": "%s",
              "title": "Backwards schedule",
              "description": "Signup window outlives the activity itself.",
              "locationLabel": "Community hall",
              "startsAt": "%s",
              "endsAt": "%s",
              "signupOpensAt": "%s",
              "signupClosesAt": "%s",
              "signupCapacity": 5
            }
            """.formatted(
                communityId,
                startsAt,
                startsAt.plusHours(2),
                startsAt.minusDays(1),
                startsAt.plusHours(1)
            );

        mockMvc.perform(post("/api/community/activities")
                .with(user("act_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());

        String zeroCapacity = """
            {
              "communityId": "%s",
              "title": "No capacity",
              "description": "A capacity of zero can never be joined.",
              "locationLabel": "Community hall",
              "startsAt": "%s",
              "endsAt": "%s",
              "signupOpensAt": "%s",
              "signupClosesAt": "%s",
              "signupCapacity": 0
            }
            """.formatted(
                communityId,
                startsAt,
                startsAt.plusHours(2),
                startsAt.minusDays(1),
                startsAt.minusHours(1)
            );

        mockMvc.perform(post("/api/community/activities")
                .with(user("act_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(zeroCapacity))
            .andExpect(status().isBadRequest());
    }

    private String createActivity(String username, String title, int capacity, int startsInHours) throws Exception {
        return createActivity(username, title, capacity, startsInHours, 200);
    }

    private String createActivity(String username, String title, int capacity, int startsInHours, int expectedStatus)
        throws Exception {
        LocalDateTime startsAt = LocalDateTime.now().plusHours(startsInHours);
        String body = """
            {
              "communityId": "%s",
              "title": "%s",
              "description": "Join the working group for this activity.",
              "locationLabel": "Community hall",
              "startsAt": "%s",
              "endsAt": "%s",
              "signupOpensAt": "%s",
              "signupClosesAt": "%s",
              "signupCapacity": %d
            }
            """.formatted(
                communityId,
                title,
                startsAt,
                startsAt.plusHours(2),
                startsAt.minusDays(1),
                startsAt.minusHours(1),
                capacity
            );

        MvcResult result = mockMvc.perform(post("/api/community/activities")
                .with(user(username).roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().is(expectedStatus))
            .andReturn();

        if (expectedStatus != 200) {
            return "";
        }
        return findActivityId(result, title);
    }

    /**
     * The board sorts activities by start time, so a test that creates several activities
     * cannot rely on index 0 being the one it just created.
     */
    private String findActivityId(MvcResult result, String title) throws Exception {
        for (com.fasterxml.jackson.databind.JsonNode node
            : new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(result.getResponse().getContentAsString()).get("activities")) {
            if (title.equals(node.get("title").asText())) {
                return node.get("id").asText();
            }
        }
        throw new AssertionError("Activity not found in board response: " + title);
    }
}