package org.opencivic.signalos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityProjectBoard;
import org.opencivic.signalos.domain.CommunityProposal;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.GovernanceDocument;
import org.opencivic.signalos.domain.GovernanceDocumentType;
import org.opencivic.signalos.domain.GovernanceDocumentVisibility;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityProjectBoardRepository;
import org.opencivic.signalos.repository.CommunityProposalRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.GovernanceDocumentRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommunityDecisionLedgerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private CommunityProposalRepository proposalRepository;
    @Autowired private GovernanceDocumentRepository governanceDocumentRepository;
    @Autowired private CommunityProjectBoardRepository projectBoardRepository;

    private UUID communityId;
    private UUID proposalId;
    private UUID documentId;
    private UUID projectBoardId;

    @BeforeEach
    void setUp() {
        User coordinator = new User("decision_coord", "{noop}pw", "decision-coord@test.dev", "ROLE_CITIZEN");
        coordinator.setEnabled(true);
        coordinator.setVerified(true);
        coordinator = userRepository.save(coordinator);

        User liaison = new User("decision_liaison", "{noop}pw", "decision-liaison@test.dev", "ROLE_CITIZEN");
        liaison.setEnabled(true);
        liaison.setVerified(true);
        liaison = userRepository.save(liaison);

        Community community = new Community();
        community.setName("Decision District");
        community.setSlug("decision-district");
        community.setDescription("Decision ledger scope");
        community = communityRepository.save(community);
        communityId = community.getId();

        CommunityMembership coordinatorMembership = new CommunityMembership();
        coordinatorMembership.setCommunityId(communityId);
        coordinatorMembership.setUserId(coordinator.getId());
        coordinatorMembership.setRole(CommunityRole.COORDINATOR);
        coordinatorMembership.setCreatedBy(coordinator.getId());
        membershipRepository.save(coordinatorMembership);

        CommunityMembership liaisonMembership = new CommunityMembership();
        liaisonMembership.setCommunityId(communityId);
        liaisonMembership.setUserId(liaison.getId());
        liaisonMembership.setRole(CommunityRole.PUBLIC_SERVANT_LIAISON);
        liaisonMembership.setCreatedBy(coordinator.getId());
        membershipRepository.save(liaisonMembership);

        CommunityProposal proposal = new CommunityProposal();
        proposal.setCommunityId(communityId);
        proposal.setAuthorId(coordinator.getId());
        proposal.setTitle("Approve safer crossing rollout");
        proposal.setProblemStatement("Families need a protected school crossing before the next rainy season.");
        proposal.setProposedSolution("Approve the crossing rollout with paint, signage, and volunteer support.");
        proposal.setEstimatedCost("COP 18M");
        proposal.setBeneficiariesSummary("Students, families, and residents.");
        proposal.setSupportingLinks(List.of("https://example.com/proposals/crossing"));
        proposal.setCreatedAt(LocalDateTime.now().minusDays(2));
        proposal.setUpdatedAt(LocalDateTime.now().minusDays(1));
        proposal = proposalRepository.save(proposal);
        proposalId = proposal.getId();

        GovernanceDocument document = new GovernanceDocument();
        document.setCommunityId(communityId);
        document.setCreatedBy(coordinator.getId());
        document.setTitle("Assembly act approving the safer crossing");
        document.setSummary("Minutes capturing the formal approval basis for the safer crossing rollout.");
        document.setDocumentType(GovernanceDocumentType.MINUTES);
        document.setVisibility(GovernanceDocumentVisibility.COMMUNITY);
        document.setTags(List.of("assembly", "crossing"));
        document.setCurrentVersionNumber(1);
        document.setCreatedAt(LocalDateTime.now().minusDays(2));
        document.setUpdatedAt(LocalDateTime.now().minusDays(1));
        document = governanceDocumentRepository.save(document);
        documentId = document.getId();

        CommunityProjectBoard board = new CommunityProjectBoard();
        board.setCommunityId(communityId);
        board.setLinkedProposalId(proposalId);
        board.setTitle("Crossing execution board");
        board.setSummary("Track procurement, volunteer shifts, and final site walkthrough.");
        board.setOwnerId(liaison.getId());
        board.setCreatedAt(LocalDateTime.now().minusDays(1));
        board.setUpdatedAt(LocalDateTime.now().minusDays(1));
        board = projectBoardRepository.save(board);
        projectBoardId = board.getId();
    }

    @Test
    @WithMockUser(username = "decision_coord", roles = {"CITIZEN"})
    void createsAndReadsDecisionLedgerEntriesWithLinkedExecutionContext() throws Exception {
        String decisionId = objectMapper.readTree(
            mockMvc.perform(post("/api/community/decisions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.ofEntries(
                        Map.entry("communityId", communityId),
                        Map.entry("linkedProposalId", proposalId),
                        Map.entry("governanceDocumentId", documentId),
                        Map.entry("projectBoardId", projectBoardId),
                        Map.entry("executionOwnerUsername", "decision_liaison"),
                        Map.entry("title", "Approve the safer crossing rollout"),
                        Map.entry("summary", "Assembly approved the crossing rollout and assigned delivery follow-through to the liaison team."),
                        Map.entry("decisionType", "APPROVAL"),
                        Map.entry("decisionStatus", "IN_EXECUTION"),
                        Map.entry("approvalBasisType", "GOVERNANCE_RECORD"),
                        Map.entry("approvalBasisSummary", "Assembly minutes recorded unanimous approval and delegated execution tracking to the liaison team.")
                    ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkedProposalId").value(proposalId.toString()))
                .andExpect(jsonPath("$.governanceDocumentId").value(documentId.toString()))
                .andExpect(jsonPath("$.projectBoardId").value(projectBoardId.toString()))
                .andExpect(jsonPath("$.executionOwnerUsername").value("decision_liaison"))
                .andExpect(jsonPath("$.decisionStatus").value("IN_EXECUTION"))
                .andReturn()
                .getResponse()
                .getContentAsString()
        ).get("id").asText();

        mockMvc.perform(get("/api/community/decisions")
                .param("communityId", communityId.toString())
                .param("decisionStatus", "IN_EXECUTION"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Approve the safer crossing rollout"))
            .andExpect(jsonPath("$[0].linkedProposalTitle").value("Approve safer crossing rollout"))
            .andExpect(jsonPath("$[0].projectBoardTitle").value("Crossing execution board"));

        mockMvc.perform(get("/api/community/decisions/{decisionId}", decisionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvalBasisType").value("GOVERNANCE_RECORD"))
            .andExpect(jsonPath("$.governanceDocumentTitle").value("Assembly act approving the safer crossing"))
            .andExpect(jsonPath("$.decidedByUsername").value("decision_coord"));
    }
}
