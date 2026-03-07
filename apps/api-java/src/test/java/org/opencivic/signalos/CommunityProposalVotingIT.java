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
import org.opencivic.signalos.domain.CommunityProposal;
import org.opencivic.signalos.domain.CommunityProposalVoteEligibility;
import org.opencivic.signalos.domain.CommunityProposalVoteMode;
import org.opencivic.signalos.domain.CommunityProposalVoteVisibility;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommunityProposalVotingIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private CommunityProposalRepository proposalRepository;

    private UUID proposalId;

    @BeforeEach
    void setUp() {
        User coordinator = new User("vote_coord", "{noop}pw", "vote-coord@test.dev", "ROLE_CITIZEN");
        coordinator.setEnabled(true);
        coordinator.setVerified(true);
        coordinator = userRepository.save(coordinator);

        User verifiedMember = new User("vote_member", "{noop}pw", "vote-member@test.dev", "ROLE_CITIZEN");
        verifiedMember.setEnabled(true);
        verifiedMember.setVerified(true);
        verifiedMember = userRepository.save(verifiedMember);

        User unverifiedMember = new User("vote_unverified", "{noop}pw", "vote-unverified@test.dev", "ROLE_CITIZEN");
        unverifiedMember.setEnabled(true);
        unverifiedMember.setVerified(false);
        unverifiedMember = userRepository.save(unverifiedMember);

        Community community = new Community();
        community.setName("Voting District");
        community.setSlug("voting-district");
        community.setDescription("Voting scope test");
        community = communityRepository.save(community);

        membershipRepository.save(createMembership(community.getId(), coordinator.getId(), coordinator.getId(), CommunityRole.COORDINATOR));
        membershipRepository.save(createMembership(community.getId(), verifiedMember.getId(), coordinator.getId(), CommunityRole.MEMBER));
        membershipRepository.save(createMembership(community.getId(), unverifiedMember.getId(), coordinator.getId(), CommunityRole.MEMBER));

        CommunityProposal proposal = new CommunityProposal();
        proposal.setCommunityId(community.getId());
        proposal.setAuthorId(coordinator.getId());
        proposal.setTitle("Approve safer school crossing");
        proposal.setProblemStatement("Families need a formal decision mechanism to prioritize safer crossing works before the school term.");
        proposal.setProposedSolution("Run a verified member vote and publish the tally before the board enters execution.");
        proposal.setEstimatedCost("COP 18M with signage, paint, and volunteers.");
        proposal.setBeneficiariesSummary("Students, caregivers, school staff, and nearby residents.");
        proposal.setSupportingLinks(List.of("https://example.com/proposal/vote"));
        proposal.setVoteMode(CommunityProposalVoteMode.YES_NO);
        proposal.setVoteVisibility(CommunityProposalVoteVisibility.AFTER_VOTE);
        proposal.setVoteEligibility(CommunityProposalVoteEligibility.VERIFIED_MEMBERS);
        proposal.setVotingOpensAt(LocalDateTime.now().minusDays(1));
        proposal.setVotingClosesAt(LocalDateTime.now().plusDays(7));
        proposal.setCreatedAt(LocalDateTime.now().minusDays(2));
        proposal.setUpdatedAt(LocalDateTime.now().minusDays(1));
        proposal = proposalRepository.save(proposal);
        proposalId = proposal.getId();
    }

    @Test
    @WithMockUser(username = "vote_member", roles = {"CITIZEN"})
    void castsVoteAndUnlocksAfterVoteTally() throws Exception {
        mockMvc.perform(get("/api/community/proposals/{proposalId}/vote", proposalId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.canCurrentUserVote").value(true))
            .andExpect(jsonPath("$.tally.visible").value(false))
            .andExpect(jsonPath("$.config.resultVisibility").value("AFTER_VOTE"));

        mockMvc.perform(post("/api/community/proposals/{proposalId}/vote", proposalId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("choice", "FOR"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentUserVote.choice").value("FOR"))
            .andExpect(jsonPath("$.tally.visible").value(true))
            .andExpect(jsonPath("$.tally.forVotes").value(1))
            .andExpect(jsonPath("$.auditSummary.acceptedVotes").value(1));

        mockMvc.perform(post("/api/community/proposals/{proposalId}/vote", proposalId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("choice", "FOR"))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("User has already voted on this community proposal."));

        mockMvc.perform(get("/api/community/proposals/{proposalId}/vote", proposalId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.canCurrentUserVote").value(false))
            .andExpect(jsonPath("$.blockedReason").value("You already voted on this proposal."))
            .andExpect(jsonPath("$.auditSummary.duplicateBlockedAttempts").value(1));
    }

    @Test
    @WithMockUser(username = "vote_unverified", roles = {"CITIZEN"})
    void blocksUnverifiedMemberAndAuditsEligibilityFailure() throws Exception {
        mockMvc.perform(post("/api/community/proposals/{proposalId}/vote", proposalId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("choice", "AGAINST"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Verified membership is required before voting on this proposal."));

        mockMvc.perform(get("/api/community/proposals/{proposalId}/vote", proposalId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.canCurrentUserVote").value(false))
            .andExpect(jsonPath("$.blockedReason").value("This proposal only accepts votes from verified members."))
            .andExpect(jsonPath("$.auditSummary.eligibilityBlockedAttempts").value(1));
    }

    private CommunityMembership createMembership(UUID communityId, UUID userId, UUID createdBy, CommunityRole role) {
        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(userId);
        membership.setRole(role);
        membership.setCreatedBy(createdBy);
        return membership;
    }
}
