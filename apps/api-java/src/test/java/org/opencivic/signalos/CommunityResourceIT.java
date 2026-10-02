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
    "spring.datasource.url=jdbc:h2:mem:resourcesit;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommunityResourceIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;

    private UUID communityId;
    private UUID coordinatorId;

    @BeforeEach
    void setUp() {
        coordinatorId = saveUser("res_coord", "Resource Coordinator");
        saveUser("res_member", "Resource Member");

        Community community = new Community();
        community.setName("Resources District");
        community.setSlug("resources-district");
        community.setDescription("Shared resource booking");
        communityId = communityRepository.save(community).getId();

        addMembership(coordinatorId, CommunityRole.COORDINATOR);
        addMembership(userRepository.findByUsername("res_member").orElseThrow().getId(), CommunityRole.MEMBER);
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
        membership.setCreatedBy(coordinatorId);
        membershipRepository.save(membership);
    }

    @Test
    void openResourceShouldAutoApproveAndAppearOnBoard() throws Exception {
        String resourceId = createResource("res_coord", "Community hall", false, 0, 4);

        mockMvc.perform(post("/api/community/resources/bookings")
                .with(user("res_member").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(resourceId, 2, 3)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.resourceName").value("Community hall"))
            .andExpect(jsonPath("$.requesterName").value("Resource Member"));

        mockMvc.perform(get("/api/community/resources/board")
                .with(user("res_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resources", hasSize(1)))
            .andExpect(jsonPath("$.resources[0].upcomingBlockingCount").value(1))
            .andExpect(jsonPath("$.resources[0].requiresApproval").value(false))
            .andExpect(jsonPath("$.myBookings", hasSize(1)));
    }

    @Test
    void overlappingBookingShouldBeRejected() throws Exception {
        String resourceId = createResource("res_coord", "Sound kit", false, 0, 4);

        mockMvc.perform(post("/api/community/resources/bookings")
                .with(user("res_member").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(resourceId, 2, 4)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/community/resources/bookings")
                .with(user("res_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(resourceId, 3, 5)))
            .andExpect(status().isConflict());

        // Back-to-back bookings touch but do not overlap, so they must both be allowed.
        mockMvc.perform(post("/api/community/resources/bookings")
                .with(user("res_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(resourceId, 4, 6)))
            .andExpect(status().isOk());
    }

    @Test
    void approvalQueueShouldBeVisibleToManagerOnly() throws Exception {
        String resourceId = createResource("res_coord", "Projector", true, 0, 4);

        mockMvc.perform(post("/api/community/resources/bookings")
                .with(user("res_member").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(resourceId, 2, 3)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
            .andExpect(jsonPath("$.decidedAt").doesNotExist());

        mockMvc.perform(get("/api/community/resources/board")
                .with(user("res_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvalsQueue", hasSize(0)))
            .andExpect(jsonPath("$.pendingApprovals").value(0));

        mockMvc.perform(get("/api/community/resources/board")
                .with(user("res_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvalsQueue", hasSize(1)))
            .andExpect(jsonPath("$.pendingApprovals").value(1));
    }

    @Test
    void requesterShouldSeeDecisionAndManagerShouldRecordIt() throws Exception {
        String resourceId = createResource("res_coord", "Meeting room", true, 0, 4);

        MvcResult request = mockMvc.perform(post("/api/community/resources/bookings")
                .with(user("res_member").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(resourceId, 2, 3)))
            .andExpect(status().isOk())
            .andReturn();
        String bookingId = json(request).get("id").asText();

        mockMvc.perform(patch("/api/community/resources/bookings")
                .with(user("res_member").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"communityId":"%s","bookingId":"%s","approve":true,"decisionNote":"Approved for the assembly"}
                    """.formatted(communityId, bookingId)))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/community/resources/bookings")
                .with(user("res_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"communityId":"%s","bookingId":"%s","approve":true,"decisionNote":"Approved for the assembly"}
                    """.formatted(communityId, bookingId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.decisionNote").value("Approved for the assembly"))
            .andExpect(jsonPath("$.decidedByName").value("Resource Coordinator"));

        mockMvc.perform(patch("/api/community/resources/bookings")
                .with(user("res_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"communityId":"%s","bookingId":"%s","approve":false}
                    """.formatted(communityId, bookingId)))
            .andExpect(status().isConflict());

        mockMvc.perform(get("/api/community/resources/board")
                .with(user("res_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.myBookings[0].status").value("APPROVED"))
            .andExpect(jsonPath("$.myBookings[0].decisionNote").value("Approved for the assembly"));
    }

    @Test
    void noticeAndDurationRulesShouldBeEnforced() throws Exception {
        String resourceId = createResource("res_coord", "Generator", false, 24, 4);

        mockMvc.perform(post("/api/community/resources/bookings")
                .with(user("res_member").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(resourceId, 1, 2)))
            .andExpect(status().isConflict());

        mockMvc.perform(post("/api/community/resources/bookings")
                .with(user("res_member").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(resourceId, 48, 60)))
            .andExpect(status().isConflict());
    }

    @Test
    void cancelledBookingShouldFreeTheWindow() throws Exception {
        String resourceId = createResource("res_coord", "Tables and chairs", false, 0, 4);

        MvcResult first = mockMvc.perform(post("/api/community/resources/bookings")
                .with(user("res_member").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(resourceId, 2, 4)))
            .andExpect(status().isOk())
            .andReturn();
        String bookingId = json(first).get("id").asText();

        mockMvc.perform(delete("/api/community/resources/bookings/{bookingId}", bookingId)
                .with(user("res_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(post("/api/community/resources/bookings")
                .with(user("res_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(resourceId, 2, 4)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void archivedResourceShouldNotAcceptBookings() throws Exception {
        String resourceId = createResource("res_coord", "Old projector", false, 0, 4);

        mockMvc.perform(delete("/api/community/resources/{resourceId}", resourceId)
                .with(user("res_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resources", hasSize(0)));

        mockMvc.perform(post("/api/community/resources/bookings")
                .with(user("res_member").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(resourceId, 2, 3)))
            .andExpect(status().isConflict());
    }

    @Test
    void memberShouldNotBeAbleToCreateResource() throws Exception {
        createResource("res_member", "Unauthorized resource", false, 0, 4, 403);
    }

    private com.fasterxml.jackson.databind.JsonNode json(MvcResult result) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(result.getResponse().getContentAsString());
    }

    private String bookingBody(String resourceId, int startsInHours, int endsInHours) {
        return """
            {
              "communityId": "%s",
              "resourceId": "%s",
              "purpose": "Neighbourhood assembly",
              "startsAt": "%s",
              "endsAt": "%s"
            }
            """.formatted(
                communityId,
                resourceId,
                LocalDateTime.now().plusHours(startsInHours),
                LocalDateTime.now().plusHours(endsInHours)
            );
    }

    private String createResource(String username, String name, boolean requiresApproval, int minNotice, int maxHours)
        throws Exception {
        return createResource(username, name, requiresApproval, minNotice, maxHours, 200);
    }

    private String createResource(
        String username,
        String name,
        boolean requiresApproval,
        int minNotice,
        int maxHours,
        int expectedStatus
    ) throws Exception {
        String body = """
            {
              "communityId": "%s",
              "name": "%s",
              "description": "Shared civic resource available for community use.",
              "locationLabel": "Community hall",
              "requiresApproval": %s,
              "minNoticeHours": %d,
              "maxBookingHours": %d
            }
            """.formatted(communityId, name, requiresApproval, minNotice, maxHours);

        MvcResult result = mockMvc.perform(post("/api/community/resources")
                .with(user(username).roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().is(expectedStatus))
            .andReturn();

        if (expectedStatus != 200) {
            return "";
        }
        for (com.fasterxml.jackson.databind.JsonNode node : json(result).get("resources")) {
            if (name.equals(node.get("name").asText())) {
                return node.get("id").asText();
            }
        }
        throw new AssertionError("Resource not found in board response: " + name);
    }
}