package org.opencivic.signalos;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:integrationsit;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommunityIntegrationIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;

    private HttpServer stub;
    private String stubBaseUri;
    private final AtomicInteger stubHits = new AtomicInteger();
    private final AtomicReference<String> lastBody = new AtomicReference<>("");
    private final AtomicReference<String> lastContentType = new AtomicReference<>("");
    private final AtomicInteger stubStatus = new AtomicInteger(200);

    private UUID communityId;
    private UUID coordinatorId;

    @BeforeEach
    void setUp() throws Exception {
        stub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        stub.createContext("/ok", exchange -> respond(exchange, 200));
        stub.createContext("/fail", exchange -> respond(exchange, stubStatus.get()));
        stub.start();
        stubBaseUri = "http://127.0.0.1:" + stub.getAddress().getPort();
        stubHits.set(0);
        lastBody.set("");
        stubStatus.set(200);

        coordinatorId = saveUser("int_coord", "Integration Coordinator");
        saveUser("int_member", "Integration Member");

        Community community = new Community();
        community.setName("Integrations District");
        community.setSlug("integrations-district");
        community.setDescription("Outbound integrations");
        communityId = communityRepository.save(community).getId();

        addMembership(coordinatorId, CommunityRole.COORDINATOR);
        addMembership(userRepository.findByUsername("int_member").orElseThrow().getId(), CommunityRole.MEMBER);
    }

    @AfterEach
    void tearDown() {
        if (stub != null) {
            stub.stop(0);
        }
    }

    private void respond(com.sun.net.httpserver.HttpExchange exchange, int status) throws java.io.IOException {
        stubHits.incrementAndGet();
        lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        lastContentType.set(String.valueOf(exchange.getRequestHeaders().getFirst("Content-Type")));
        byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
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
    void announcementShouldFanOutToWebhookAndRecordSuccess() throws Exception {
        createIntegration("WEBHOOK", "Neighbourhood board feed", stubBaseUri + "/ok", 200);

        mockMvc.perform(post("/api/community/integrations/events")
                .with(user("int_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventBody("OFFICIAL_ANNOUNCEMENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.targeted").value(1))
            .andExpect(jsonPath("$.delivered").value(1))
            .andExpect(jsonPath("$.failed").value(0))
            .andExpect(jsonPath("$.deliveries", hasSize(1)))
            .andExpect(jsonPath("$.deliveries[0].status").value("DELIVERED"))
            .andExpect(jsonPath("$.deliveries[0].attempts").value(1));

        org.junit.jupiter.api.Assertions.assertEquals(1, stubHits.get());
        org.junit.jupiter.api.Assertions.assertTrue(lastBody.get().contains("OFFICIAL_ANNOUNCEMENT"));
        org.junit.jupiter.api.Assertions.assertEquals("application/json", lastContentType.get());

        mockMvc.perform(get("/api/community/integrations/center")
                .with(user("int_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.integrationCount").value(1))
            .andExpect(jsonPath("$.failedDeliveries").value(0))
            .andExpect(jsonPath("$.recentDeliveries[0].status").value("DELIVERED"))
            .andExpect(jsonPath("$.integrations[0].dispatchable").value(true));
    }

    @Test
    void failingEndpointShouldBeVisibleAndRetryable() throws Exception {
        stubStatus.set(500);
        createIntegration("WEBHOOK", "Broken feed", stubBaseUri + "/fail", 200);

        mockMvc.perform(post("/api/community/integrations/events")
                .with(user("int_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventBody("OFFICIAL_ANNOUNCEMENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.delivered").value(0))
            .andExpect(jsonPath("$.failed").value(1))
            .andExpect(jsonPath("$.deliveries[0].status").value("FAILED"))
            .andExpect(jsonPath("$.deliveries[0].lastError").value("Endpoint returned HTTP 500"));

        String deliveryId = deliveryIdFromCenter();

        mockMvc.perform(get("/api/community/integrations/center")
                .with(user("int_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.failedDeliveries").value(1))
            .andExpect(jsonPath("$.integrations[0].consecutiveFailures").value(1));

        stubStatus.set(200);
        mockMvc.perform(post("/api/community/integrations/deliveries/{deliveryId}/retry", deliveryId)
                .with(user("int_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DELIVERED"))
            .andExpect(jsonPath("$.attempts").value(2));

        mockMvc.perform(get("/api/community/integrations/center")
                .with(user("int_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.integrations[0].consecutiveFailures").value(0));
    }

    @Test
    void calendarFeedShouldOnlyReceiveScheduledEvents() throws Exception {
        createIntegration("CALENDAR_FEED", "Calendar feed", stubBaseUri + "/ok", 200);

        mockMvc.perform(post("/api/community/integrations/events")
                .with(user("int_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventBody("OFFICIAL_ANNOUNCEMENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.targeted").value(0));

        mockMvc.perform(post("/api/community/integrations/events")
                .with(user("int_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventBody("ACTIVITY_SCHEDULED")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.targeted").value(1))
            .andExpect(jsonPath("$.delivered").value(1));

        org.junit.jupiter.api.Assertions.assertTrue(lastBody.get().startsWith("BEGIN:VCALENDAR"));
        org.junit.jupiter.api.Assertions.assertEquals("text/calendar; charset=utf-8", lastContentType.get());
    }

    @Test
    void channelWithoutConnectorShouldFailVisibly() throws Exception {
        createIntegration("EMAIL_DIGEST", "Email digest", stubBaseUri + "/ok", 200);

        mockMvc.perform(post("/api/community/integrations/events")
                .with(user("int_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventBody("OFFICIAL_ANNOUNCEMENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.skippedNoConnector").value(1))
            .andExpect(jsonPath("$.deliveries[0].status").value("FAILED"))
            .andExpect(jsonPath("$.deliveries[0].lastError")
                .value("No connector is registered for this channel yet."));

        org.junit.jupiter.api.Assertions.assertEquals(0, stubHits.get());
    }

    @Test
    void disabledIntegrationShouldNotReceiveEvents() throws Exception {
        String integrationId = createIntegration("WEBHOOK", "Paused feed", stubBaseUri + "/ok", 200);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/api/community/integrations/{integrationId}", integrationId)
                .with(user("int_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("enabled", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(post("/api/community/integrations/events")
                .with(user("int_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventBody("OFFICIAL_ANNOUNCEMENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.targeted").value(0));

        org.junit.jupiter.api.Assertions.assertEquals(0, stubHits.get());
    }

    @Test
    void memberShouldNotManageIntegrations() throws Exception {
        mockMvc.perform(get("/api/community/integrations/center")
                .with(user("int_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isForbidden());

        createIntegrationAs("int_member", "WEBHOOK", "Unauthorized", stubBaseUri + "/ok", 403);
    }

    @Test
    void invalidTargetShouldBeRejected() throws Exception {
        createIntegration("WEBHOOK", "No scheme", "not-a-url", 400);
        createIntegration("WEBHOOK", "Short secret", stubBaseUri + "/ok", 400);
    }

    private String deliveryIdFromCenter() throws Exception {
        String body = mockMvc.perform(get("/api/community/integrations/center")
                .with(user("int_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(body).get("recentDeliveries").get(0).get("id").asText();
    }

    private String eventBody(String eventType) {
        LocalDateTime startsAt = LocalDateTime.now().plusDays(1);
        return """
            {
              "communityId": "%s",
              "eventType": "%s",
              "referenceId": "%s",
              "title": "Night patrol briefing",
              "description": "Coordination briefing for the working group",
              "locationLabel": "Community hall",
              "startsAt": "%s",
              "endsAt": "%s"
            }
            """.formatted(communityId, eventType, UUID.randomUUID(), startsAt, startsAt.plusHours(2));
    }

    private String createIntegration(String channel, String name, String targetUri, int expectedStatus)
        throws Exception {
        return createIntegrationAs("int_coord", channel, name, targetUri, expectedStatus);
    }

    private String createIntegrationAs(
        String username,
        String channel,
        String name,
        String targetUri,
        int expectedStatus
    ) throws Exception {
        boolean shortSecret = "Short secret".equals(name);
        String body = """
            {
              "communityId": "%s",
              "channel": "%s",
              "name": "%s",
              "targetUri": "%s",
              "secret": "%s"
            }
            """.formatted(
                communityId, channel, name, targetUri,
                shortSecret ? "short" : "a-sufficiently-long-signing-secret"
            );

        var result = mockMvc.perform(post("/api/community/integrations")
                .with(user(username).roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().is(expectedStatus))
            .andReturn();

        if (expectedStatus != 200) {
            return "";
        }
        return new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}