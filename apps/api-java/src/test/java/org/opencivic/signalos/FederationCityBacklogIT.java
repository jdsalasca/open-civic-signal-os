package org.opencivic.signalos;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityOpenDataPolicy;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Two cities, one instance: the federation acceptance criterion, tested rather than asserted.
 *
 * <p>"Two city datasets can coexist without conflict" is easy to claim and easy to get wrong, because
 * the failure is not a crash. It is one city's backlog quietly containing another city's reports,
 * which looks like a working feature to anybody who only reads one city's dashboard.
 *
 * <p>Every test here therefore asks a cross-city question: does anything from the other city appear?
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:federationcities;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FederationCityBacklogIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private SignalRepository signalRepository;

    private UUID coordinatorId;
    private String coordinatorName;
    private UUID riversideId;
    private UUID northgateId;
    private String riversideKey;

    @BeforeEach
    void setUp() {
        // Unique per method: this class is not transactional, so its in-memory database keeps rows
        // between tests and both username and email are unique columns.
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User coordinator = new User("fed_coord_" + unique, "encoded", "fed_" + unique + "@example.com", "ROLE_CITIZEN");
        coordinator.setVerified(true);
        coordinator.setEnabled(true);
        coordinatorName = coordinator.getUsername();
        coordinatorId = userRepository.save(coordinator).getId();

        Community riverside = community("Riverside District", "riverside-federation");
        Community northgate = community("Northgate", "northgate-federation");
        riversideId = riverside.getId();
        northgateId = northgate.getId();
        riversideKey = riverside.getFederationKey();

        membership(coordinatorId, riversideId);
        membership(coordinatorId, northgateId);

        signal(riversideId, "Riverside water main break", 313.0, "NEW", "Riverside");
        signal(riversideId, "Riverside streetlight out", 120.0, "NEW", "Riverside");
        signal(northgateId, "Northgate playground fence", 400.0, "NEW", "Northgate");
        signal(northgateId, "Northgate pothole cluster", 90.0, "NEW", "Northgate");
    }

    @Test
    void oneCitysBacklogNeverContainsAnotherCitysReports() throws Exception {
        String token = token(riversideId);

        String body = mockMvc.perform(get("/api/open-data/{communityId}/prioritized_backlog", riversideId)
                .header("X-Api-Token", token))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Riverside water main break")))
            .andReturn().getResponse().getContentAsString();

        // The whole point of scoping. A backlog that mixed cities would still look right to anyone
        // reading only Riverside's dashboard.
        org.junit.jupiter.api.Assertions.assertFalse(
            body.contains("Northgate"),
            "a city's federated backlog must not contain another city's reports: " + body);

        JsonNode rows = objectMapper.readTree(body);
        org.junit.jupiter.api.Assertions.assertEquals(2, rows.size());
        // Highest first, so a peer can render the top of the list without sorting.
        org.junit.jupiter.api.Assertions.assertEquals("Riverside water main break", rows.get(0).path("title").asText());
        org.junit.jupiter.api.Assertions.assertEquals(1, rows.get(0).path("rank").asInt());
    }

    @Test
    void bothCitiesGetTheirOwnKeyAndBothResolveIndependently() throws Exception {
        org.junit.jupiter.api.Assertions.assertNotNull(riversideKey);
        org.junit.jupiter.api.Assertions.assertNotEquals(
            riversideKey,
            communityRepository.findById(northgateId).orElseThrow().getFederationKey());

        String riverside = mockMvc.perform(get("/api/open-data/{communityId}/prioritized_backlog", riversideId)
                .header("X-Api-Token", token(riversideId)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String northgate = mockMvc.perform(get("/api/open-data/{communityId}/prioritized_backlog", northgateId)
                .header("X-Api-Token", token(northgateId)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertTrue(riverside.contains("Riverside water main break"));
        org.junit.jupiter.api.Assertions.assertFalse(riverside.contains("Northgate"));
        org.junit.jupiter.api.Assertions.assertTrue(northgate.contains("Northgate playground fence"));
        org.junit.jupiter.api.Assertions.assertFalse(northgate.contains("Riverside"));
    }

    @Test
    void theBacklogCarriesTheFormulaSoAPeerCanExplainTheRank() throws Exception {
        String body = mockMvc.perform(get("/api/open-data/{communityId}/prioritized_backlog", riversideId)
                .header("X-Api-Token", token(riversideId)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode top = objectMapper.readTree(body).get(0);
        // The city's key travels with every row: it is the namespace a peer can rely on.
        org.junit.jupiter.api.Assertions.assertEquals(riversideKey, top.path("cityKey").asText());
        // Inputs plus the published expression, rather than a second "why" string to keep in step.
        org.junit.jupiter.api.Assertions.assertEquals(313.0, top.path("priorityScore").asDouble());
        org.junit.jupiter.api.Assertions.assertTrue(top.path("urgency").asInt() > 0);
        org.junit.jupiter.api.Assertions.assertTrue(top.path("impact").asInt() > 0);
        org.junit.jupiter.api.Assertions.assertEquals(
            "v1", top.path("formulaVersion").asText());
        org.junit.jupiter.api.Assertions.assertTrue(
            top.path("formulaExpression").asText().contains("Urgency"),
            "expected the published formula, got: " + top.path("formulaExpression").asText());
        org.junit.jupiter.api.Assertions.assertFalse(top.path("generatedAt").asText().isBlank());
    }

    @Test
    void aResolvedIssueLeavesTheFederatedBacklog() throws Exception {
        Signal resolved = signalRepository.findByCommunityId(riversideId).stream()
            .filter(signal -> "Riverside water main break".equals(signal.getTitle()))
            .findFirst()
            .orElseThrow();
        resolved.setStatus("RESOLVED");
        signalRepository.save(resolved);

        String body = mockMvc.perform(get("/api/open-data/{communityId}/prioritized_backlog", riversideId)
                .header("X-Api-Token", token(riversideId)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        // A ranked list that kept presenting a settled issue as pending would be a public claim the
        // community already decided against.
        org.junit.jupiter.api.Assertions.assertFalse(
            body.contains("Riverside water main break"),
            "a resolved issue must leave the federated backlog: " + body);
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("Riverside streetlight out"));
    }

    @Test
    void theBacklogNeedsItsOwnTokenScope() throws Exception {
        // A token scoped to SIGNALS must not read the backlog, or every existing feed token would
        // silently gain access to a dataset nobody approved.
        String signalsOnly = token(riversideId, "EXPORT_SIGNALS");

        mockMvc.perform(get("/api/open-data/{communityId}/prioritized_backlog", riversideId)
                .header("X-Api-Token", signalsOnly))
            .andExpect(status().isUnauthorized());
    }

@Test
    void theBacklogIsAlsoServedAsCsvWithItsHeader() throws Exception {
        // CSV lives on the authenticated route; the token endpoint serves JSON only.
        mockMvc.perform(get("/api/community/exports/prioritized_backlog")
                .with(user(coordinatorName).roles("CITIZEN"))
                .queryParam("communityId", riversideId.toString())
                .queryParam("format", "CSV"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("cityKey,cityName,signalId,rank")));
    }

    @Test
    void turningOpenDataOffStopsTheFeedImmediately() throws Exception {
        Community quiet = community("Quiet District", "quiet-federation");
        membership(coordinatorId, quiet.getId());
        String token = token(quiet.getId());

        // Fed while it opted in, so the refusal below is about the policy rather than the token.
        mockMvc.perform(get("/api/open-data/{communityId}/prioritized_backlog", quiet.getId())
                .header("X-Api-Token", token))
            .andExpect(status().isOk());

        quiet.setOpenDataPolicy(CommunityOpenDataPolicy.DISABLED);
        communityRepository.save(quiet);

        // A feed that keeps serving after a community withdraws consent is the failure that matters.
        mockMvc.perform(get("/api/open-data/{communityId}/prioritized_backlog", quiet.getId())
                .header("X-Api-Token", token))
            .andExpect(status().isForbidden());
    }

    private String token(UUID communityId, String... scopes) throws Exception {
        String scopeList = scopes.length == 0 ? "[\"EXPORT_PRIORITIZED_BACKLOG\"]"
            : "[\"" + String.join("\", \"", scopes) + "\"]";
        var result = mockMvc.perform(post("/api/community/exports/tokens")
                .with(user(coordinatorName).roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "label": "Peer instance",
                      "scopes": %s,
                      "rateLimitPerHour": 100
                    }
                    """.formatted(communityId, scopeList)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token.scopes", hasSize(scopes.length == 0 ? 1 : scopes.length)))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("plainToken").asText();
    }

    private Community community(String name, String slug) {
        Community community = new Community();
        community.setName(name);
        community.setSlug(slug + "-" + UUID.randomUUID().toString().substring(0, 8));
        community.setDescription("Federation city");
        community.setOpenDataPolicy(CommunityOpenDataPolicy.AGGREGATED_PUBLIC);
        return communityRepository.save(community);
    }

    private void membership(UUID userId, UUID communityId) {
        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(userId);
        membership.setRole(CommunityRole.COORDINATOR);
        membership.setCreatedBy(coordinatorId);
        membershipRepository.save(membership);
    }

    private void signal(UUID communityId, String title, double score, String status, String locationLabel) {
        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setCommunityId(communityId);
        signal.setAuthorId(coordinatorId);
        signal.setTitle(title);
        signal.setDescription("Federation backlog fixture.");
        signal.setCategory("utilities");
        signal.setStatus(status);
        signal.setUrgency(4);
        signal.setImpact(4);
        signal.setAffectedPeople(120);
        signal.setCommunityVotes(8);
        signal.setPriorityScore(score);
        signal.setScoreBreakdown(new ScoreBreakdown(4, 4, 120, 8));
        signal.setLocationLabel(locationLabel);
        signalRepository.save(signal);
    }
}