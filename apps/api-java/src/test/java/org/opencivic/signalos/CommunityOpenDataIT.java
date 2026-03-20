package org.opencivic.signalos;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityDecision;
import org.opencivic.signalos.domain.CommunityDecisionBasisType;
import org.opencivic.signalos.domain.CommunityDecisionStatus;
import org.opencivic.signalos.domain.CommunityDecisionType;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityProposal;
import org.opencivic.signalos.domain.CommunityProposalVote;
import org.opencivic.signalos.domain.CommunityProposalVoteChoice;
import org.opencivic.signalos.domain.CommunityProposalVoteMode;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityDecisionRepository;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityProposalRepository;
import org.opencivic.signalos.repository.CommunityProposalVoteRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.SignalRepository;
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
    "spring.datasource.url=jdbc:h2:mem:opendatait;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommunityOpenDataIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private SignalRepository signalRepository;
    @Autowired private CommunityProposalRepository proposalRepository;
    @Autowired private CommunityProposalVoteRepository voteRepository;
    @Autowired private CommunityDecisionRepository decisionRepository;

    private UUID communityId;
    private UUID proposalId;

    @BeforeEach
    void setUp() {
        User coordinator = new User("open_data_coord", "encoded", "exports@example.com", "ROLE_CITIZEN");
        coordinator.setVerified(true);
        coordinator.setEnabled(true);
        coordinator = userRepository.save(coordinator);

        Community community = new Community();
        community.setName("Open Data District");
        community.setSlug("open-data-district");
        community.setDescription("Interoperable exports");
        community = communityRepository.save(community);
        communityId = community.getId();

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(coordinator.getId());
        membership.setRole(CommunityRole.COORDINATOR);
        membership.setCreatedBy(coordinator.getId());
        membershipRepository.save(membership);

        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setCommunityId(communityId);
        signal.setAuthorId(coordinator.getId());
        signal.setTitle("Streetlight outage");
        signal.setDescription("Main corridor lights are out.");
        signal.setCategory("infrastructure");
        signal.setStatus("IN_PROGRESS");
        signal.setUrgency(4);
        signal.setImpact(4);
        signal.setAffectedPeople(120);
        signal.setCommunityVotes(8);
        signal.setPriorityScore(82.4);
        signal.setScoreBreakdown(new ScoreBreakdown(4, 4, 120, 8));
        signal.setLocationLabel("Main corridor");
        signal.setCreatedAt(LocalDateTime.now().minusDays(2));
        signalRepository.save(signal);

        CommunityProposal proposal = new CommunityProposal();
        proposal.setCommunityId(communityId);
        proposal.setAuthorId(coordinator.getId());
        proposal.setRelatedSignalId(signal.getId());
        proposal.setTitle("Install replacement lamps");
        proposal.setStatus("VOTING");
        proposal.setProblemStatement("Lighting failures reduce safe night mobility.");
        proposal.setProposedSolution("Install replacement lamps and add inspection rounds.");
        proposal.setEstimatedCost("USD 4000");
        proposal.setBeneficiariesSummary("Night commuters and nearby residents");
        proposal.setSupportingLinks(java.util.List.of("https://example.com/lighting-plan"));
        proposal.setVoteMode(CommunityProposalVoteMode.YES_NO);
        proposal.setCreatedAt(LocalDateTime.now().minusDays(1));
        proposal.setUpdatedAt(LocalDateTime.now().minusHours(2));
        proposal = proposalRepository.save(proposal);
        proposalId = proposal.getId();

        CommunityProposalVote vote = new CommunityProposalVote();
        vote.setCommunityId(communityId);
        vote.setProposalId(proposalId);
        vote.setVoterId(coordinator.getId());
        vote.setVoterUsername(coordinator.getUsername());
        vote.setMembershipRole(CommunityRole.COORDINATOR);
        vote.setVerifiedMember(true);
        vote.setVoteMode(CommunityProposalVoteMode.YES_NO);
        vote.setChoice(CommunityProposalVoteChoice.FOR);
        vote.setCreatedAt(LocalDateTime.now().minusHours(1));
        vote.setUpdatedAt(LocalDateTime.now().minusHours(1));
        voteRepository.save(vote);

        CommunityDecision decision = new CommunityDecision();
        decision.setCommunityId(communityId);
        decision.setLinkedProposalId(proposalId);
        decision.setDecidedBy(coordinator.getId());
        decision.setDecisionType(CommunityDecisionType.APPROVAL);
        decision.setDecisionStatus(CommunityDecisionStatus.RECORDED);
        decision.setApprovalBasisType(CommunityDecisionBasisType.COMMUNITY_VOTE);
        decision.setTitle("Approve lighting replacement plan");
        decision.setSummary("Community approved the lamp replacement plan.");
        decision.setApprovalBasisSummary("Simple majority vote passed.");
        decision.setDecidedAt(LocalDateTime.now().minusMinutes(30));
        decision.setUpdatedAt(LocalDateTime.now().minusMinutes(30));
        decisionRepository.save(decision);
    }

    @Test
    void exportCenterShouldListDatasetsAndCreateScopedToken() throws Exception {
        mockMvc.perform(get("/api/community/exports/center")
                .with(user("open_data_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.communityId").value(communityId.toString()))
            .andExpect(jsonPath("$.defaultRateLimitPerHour").value(120))
            .andExpect(jsonPath("$.datasets", hasSize(5)))
            .andExpect(jsonPath("$.datasets[?(@.resource=='SIGNALS')]").exists());

        mockMvc.perform(post("/api/community/exports/tokens")
                .with(user("open_data_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "label": "University feed",
                      "scopes": ["EXPORT_SIGNALS", "EXPORT_METRICS"],
                      "rateLimitPerHour": 1
                    }
                    """.formatted(communityId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token.label").value("University feed"))
            .andExpect(jsonPath("$.token.scopes", hasItem("EXPORT_SIGNALS")))
            .andExpect(jsonPath("$.plainToken").value(org.hamcrest.Matchers.startsWith("ocs_")));
    }

    @Test
    void exportsShouldAuditDownloadsAndEnforceTokenRateLimits() throws Exception {
        MvcResult tokenResult = mockMvc.perform(post("/api/community/exports/tokens")
                .with(user("open_data_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "label": "Municipal API",
                      "scopes": ["EXPORT_SIGNALS"],
                      "rateLimitPerHour": 1
                    }
                    """.formatted(communityId)))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode tokenPayload = objectMapper.readTree(tokenResult.getResponse().getContentAsString());
        String plainToken = tokenPayload.path("plainToken").asText();
        String tokenId = tokenPayload.path("token").path("id").asText();

        mockMvc.perform(get("/api/community/exports/signals")
                .with(user("open_data_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("format", "CSV"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Streetlight outage")));

        mockMvc.perform(get("/api/community/exports/metrics")
                .with(user("open_data_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("format", "JSON"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("LAST_30_DAYS")));

        mockMvc.perform(get("/api/open-data/{communityId}/signals", communityId)
                .header("X-Api-Token", plainToken))
            .andExpect(status().isOk())
            .andExpect(header().string("X-RateLimit-Limit", "1"))
            .andExpect(header().string("X-RateLimit-Remaining", "0"))
            .andExpect(jsonPath("$[0].title").value("Streetlight outage"));

        mockMvc.perform(get("/api/open-data/{communityId}/signals", communityId)
                .header("X-Api-Token", plainToken))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.resetAt").exists());

        mockMvc.perform(get("/api/community/exports/center")
                .with(user("open_data_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokens[0].id").value(tokenId))
            .andExpect(jsonPath("$.recentAccessLogs.length()").value(greaterThanOrEqualTo(3)))
            .andExpect(jsonPath("$.recentAccessLogs[?(@.accessChannel=='USER_EXPORT')].exportType", hasItem("SIGNALS")))
            .andExpect(jsonPath("$.recentAccessLogs[?(@.accessChannel=='API_TOKEN')].exportType", hasItem("SIGNALS")));

        mockMvc.perform(delete("/api/community/exports/tokens/{tokenId}", UUID.fromString(tokenId))
                .with(user("open_data_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));
    }
}
