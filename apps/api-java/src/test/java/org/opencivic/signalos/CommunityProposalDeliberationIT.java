package org.opencivic.signalos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.opencivic.signalos.domain.CommunityProposal;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityProposalRepository;
import org.opencivic.signalos.repository.CommunityRepository;
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
class CommunityProposalDeliberationIT {

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
    private CommunityProposalRepository proposalRepository;

    private UUID proposalId;
    private UUID communityId;
    private UUID authorId;
    private UUID coordinatorId;

    @BeforeEach
    void setUp() {
        User author = new User("proposal_member", "{noop}pw", "proposal-member@test.dev", "ROLE_CITIZEN");
        author.setEnabled(true);
        author.setVerified(true);
        author = userRepository.save(author);
        authorId = author.getId();

        User coordinator = new User("proposal_coordinator", "{noop}pw", "proposal-coordinator@test.dev", "ROLE_CITIZEN");
        coordinator.setEnabled(true);
        coordinator.setVerified(true);
        coordinator = userRepository.save(coordinator);
        coordinatorId = coordinator.getId();

        Community community = new Community();
        community.setName("Deliberation District");
        community.setSlug("deliberation-district");
        community.setDescription("Proposal deliberation scope");
        community = communityRepository.save(community);
        communityId = community.getId();

        CommunityMembership memberMembership = new CommunityMembership();
        memberMembership.setCommunityId(communityId);
        memberMembership.setUserId(authorId);
        memberMembership.setRole(CommunityRole.MEMBER);
        memberMembership.setCreatedBy(authorId);
        membershipRepository.save(memberMembership);

        CommunityMembership coordinatorMembership = new CommunityMembership();
        coordinatorMembership.setCommunityId(communityId);
        coordinatorMembership.setUserId(coordinatorId);
        coordinatorMembership.setRole(CommunityRole.COORDINATOR);
        coordinatorMembership.setCreatedBy(coordinatorId);
        membershipRepository.save(coordinatorMembership);

        CommunityProposal proposal = new CommunityProposal();
        proposal.setCommunityId(communityId);
        proposal.setAuthorId(authorId);
        proposal.setTitle("Improve the school crossing");
        proposal.setProblemStatement("Students cross without enough physical traffic calming.");
        proposal.setProposedSolution("Install a raised crossing and reflective signage.");
        proposal.setEstimatedCost("COP 18M for construction and signage.");
        proposal.setBeneficiariesSummary("Students, parents, nearby residents, and drivers.");
        proposal.setSupportingLinks(List.of("https://example.com/proposal"));
        proposal.setCreatedAt(LocalDateTime.now().minusDays(1));
        proposal.setUpdatedAt(LocalDateTime.now().minusDays(1));
        proposal = proposalRepository.save(proposal);
        proposalId = proposal.getId();
    }

    @Test
    @WithMockUser(username = "proposal_member", roles = {"CITIZEN"})
    void createsTypedEntriesAndCountsVisibleArguments() throws Exception {
        mockMvc.perform(post("/api/community/proposals/{proposalId}/deliberation", proposalId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                    "type", "PRO",
                    "content", "This intervention lowers speeds at the school gate and addresses the current safety risk."
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.counts.pros").value(1))
            .andExpect(jsonPath("$.entries[0].entryType").value("PRO"));

        mockMvc.perform(post("/api/community/proposals/{proposalId}/deliberation", proposalId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                    "type", "EVIDENCE",
                    "content", "Shared community memo with traffic observations and crossing incidents.",
                    "supportingLink", "https://example.com/evidence/school-crossing-memo"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.counts.evidence").value(1))
            .andExpect(jsonPath("$.counts.visibleEntries").value(2))
            .andExpect(jsonPath("$.entries[1].supportingLink").value("https://example.com/evidence/school-crossing-memo"));
    }

    @Test
    @WithMockUser(username = "proposal_coordinator", roles = {"CITIZEN"})
    void moderatorCanHideEntryWithoutDeletingProposalContext() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/community/proposals/{proposalId}/deliberation", proposalId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                    "type", "QUESTION",
                    "content", "Do we already know whether the transport office approved similar crossings nearby?"
                ))))
            .andExpect(status().isOk())
            .andReturn();

        String entryId = objectMapper.readTree(createResult.getResponse().getContentAsString())
            .get("entries")
            .get(0)
            .get("id")
            .asText();

        mockMvc.perform(patch("/api/community/proposals/{proposalId}/deliberation/{entryId}/moderate", proposalId, entryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                    "hidden", true,
                    "reason", "Off-topic question duplicated from a previous evidence review thread."
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.counts.visibleEntries").value(0))
            .andExpect(jsonPath("$.counts.hiddenEntries").value(1))
            .andExpect(jsonPath("$.entries[0].hidden").value(true))
            .andExpect(jsonPath("$.entries[0].moderationReason").value("Off-topic question duplicated from a previous evidence review thread."));

        mockMvc.perform(get("/api/community/proposals/{proposalId}", proposalId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Improve the school crossing"));
    }
}
