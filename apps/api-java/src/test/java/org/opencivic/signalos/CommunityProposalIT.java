package org.opencivic.signalos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityRole;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommunityProposalIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private CommunityMembershipRepository membershipRepository;

    @Autowired
    private SignalRepository signalRepository;

    private UUID communityId;
    private UUID signalId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        User user = new User("proposal_author", "{noop}pw", "proposal@test.dev", "ROLE_CITIZEN");
        user.setEnabled(true);
        user.setVerified(true);
        user = userRepository.save(user);
        userId = user.getId();

        Community community = new Community();
        community.setName("Proposal District");
        community.setSlug("proposal-district");
        community.setDescription("Proposal scope test");
        community = communityRepository.save(community);
        communityId = community.getId();

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(userId);
        membership.setRole(CommunityRole.MEMBER);
        membership.setCreatedBy(userId);
        membershipRepository.save(membership);

        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setTitle("Unsafe pedestrian crossing");
        signal.setDescription("Crossing needs intervention.");
        signal.setCategory("SAFETY");
        signal.setStatus("NEW");
        signal.setCommunityId(communityId);
        signal.setAuthorId(userId);
        signal.setCreatedAt(LocalDateTime.now().minusDays(1));
        signal.setPriorityScore(210.0);
        signal = signalRepository.save(signal);
        signalId = signal.getId();
    }

    @Test
    @WithMockUser(username = "proposal_author", roles = {"CITIZEN"})
    void createsListsAndReadsStructuredProposal() throws Exception {
        var payload = java.util.Map.of(
            "communityId", communityId,
            "relatedSignalId", signalId,
            "title", "Install raised pedestrian crossing near the school",
            "problemStatement", "Families and students cross at high traffic hours without enough physical slowing measures.",
            "proposedSolution", "Build a raised crossing with signage and reflective paint in front of the school entrance.",
            "estimatedCost", "Estimated municipal cost: COP 18M for civil works, signage, and paint.",
            "beneficiariesSummary", "Students, parents, nearby residents, and drivers using the corridor every day.",
            "supportingLinks", List.of("https://example.com/evidence/crossing-photo", "https://example.com/docs/school-request")
        );

        MvcResult createResult = mockMvc.perform(
                post("/api/community/proposals")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.communityId").value(communityId.toString()))
            .andExpect(jsonPath("$.relatedSignalId").value(signalId.toString()))
            .andExpect(jsonPath("$.relatedSignalTitle").value("Unsafe pedestrian crossing"))
            .andExpect(jsonPath("$.templateKey").value("STANDARD_COMMUNITY_PROPOSAL"))
            .andExpect(jsonPath("$.supportingLinks.length()").value(2))
            .andReturn();

        String proposalId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(
                get("/api/community/proposals")
                    .param("communityId", communityId.toString())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(proposalId))
            .andExpect(jsonPath("$[0].title").value("Install raised pedestrian crossing near the school"));

        mockMvc.perform(get("/api/community/proposals/{proposalId}", proposalId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.problemStatement").value("Families and students cross at high traffic hours without enough physical slowing measures."))
            .andExpect(jsonPath("$.proposedSolution").value("Build a raised crossing with signage and reflective paint in front of the school entrance."))
            .andExpect(jsonPath("$.estimatedCost").value("Estimated municipal cost: COP 18M for civil works, signage, and paint."))
            .andExpect(jsonPath("$.beneficiariesSummary").value("Students, parents, nearby residents, and drivers using the corridor every day."));
    }
}
