package org.opencivic.signalos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityProjectBoardRepository;
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
class CommunityProjectBoardIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private CommunityProposalRepository proposalRepository;
    @Autowired private CommunityProjectBoardRepository boardRepository;

    private UUID communityId;
    private UUID proposalId;

    @BeforeEach
    void setUp() {
        User coordinator = new User("project_coordinator", "{noop}pw", "project-coordinator@test.dev", "ROLE_CITIZEN");
        coordinator.setEnabled(true);
        coordinator.setVerified(true);
        coordinator = userRepository.save(coordinator);

        User member = new User("project_member", "{noop}pw", "project-member@test.dev", "ROLE_CITIZEN");
        member.setEnabled(true);
        member.setVerified(true);
        member = userRepository.save(member);

        Community community = new Community();
        community.setName("Project District");
        community.setSlug("project-district");
        community.setDescription("Execution board scope");
        community = communityRepository.save(community);
        communityId = community.getId();

        CommunityMembership coordinatorMembership = new CommunityMembership();
        coordinatorMembership.setCommunityId(communityId);
        coordinatorMembership.setUserId(coordinator.getId());
        coordinatorMembership.setRole(CommunityRole.COORDINATOR);
        coordinatorMembership.setCreatedBy(coordinator.getId());
        membershipRepository.save(coordinatorMembership);

        CommunityMembership memberMembership = new CommunityMembership();
        memberMembership.setCommunityId(communityId);
        memberMembership.setUserId(member.getId());
        memberMembership.setRole(CommunityRole.MEMBER);
        memberMembership.setCreatedBy(coordinator.getId());
        membershipRepository.save(memberMembership);

        CommunityProposal proposal = new CommunityProposal();
        proposal.setCommunityId(communityId);
        proposal.setAuthorId(coordinator.getId());
        proposal.setTitle("Paint the crossing mural");
        proposal.setProblemStatement("The school edge feels unsafe and visually neglected.");
        proposal.setProposedSolution("Coordinate one mural and street-marking intervention with neighbors.");
        proposal.setEstimatedCost("COP 5M in paint, barriers, and volunteer support.");
        proposal.setBeneficiariesSummary("Students, nearby residents, and daily pedestrians.");
        proposal.setSupportingLinks(List.of("https://example.com/proposal/mural"));
        proposal.setCreatedAt(LocalDateTime.now().minusDays(1));
        proposal.setUpdatedAt(LocalDateTime.now().minusDays(1));
        proposal = proposalRepository.save(proposal);
        proposalId = proposal.getId();
    }

    @Test
    @WithMockUser(username = "project_coordinator", roles = {"CITIZEN"})
    void createsBoardAddsTaskMovesTaskAndKeepsCommentsOnTask() throws Exception {
        String createBoardBody = objectMapper.writeValueAsString(Map.of(
            "communityId", communityId,
            "linkedProposalId", proposalId,
            "title", "Crossing mural execution board",
            "summary", "Track approvals, paint prep, volunteer shifts, and final installation."
        ));

        String boardId = objectMapper.readTree(
            mockMvc.perform(post("/api/community/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createBoardBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Crossing mural execution board"))
                .andExpect(jsonPath("$.linkedProposalId").value(proposalId.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString()
        ).get("id").asText();

        String createTaskBody = objectMapper.writeValueAsString(Map.of(
            "title", "Confirm mural paint palette",
            "details", "Close the palette with the school committee and buy materials for the pilot day.",
            "assigneeUsername", "project_member"
        ));

        String taskId = objectMapper.readTree(
            mockMvc.perform(post("/api/community/projects/{projectId}/tasks", boardId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createTaskBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskCounts.todo").value(1))
                .andExpect(jsonPath("$.tasks[0].status").value("TODO"))
                .andReturn()
                .getResponse()
                .getContentAsString()
        ).get("tasks").get(0).get("id").asText();

        mockMvc.perform(patch("/api/community/projects/{projectId}/tasks/{taskId}", boardId, taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", "Confirm mural paint palette",
                    "details", "Palette aligned with school committee and procurement notes attached.",
                    "status", "IN_PROGRESS",
                    "assigneeUsername", "project_member"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taskCounts.inProgress").value(1))
            .andExpect(jsonPath("$.tasks[0].status").value("IN_PROGRESS"));

        mockMvc.perform(post("/api/community/projects/{projectId}/tasks/{taskId}/comments", boardId, taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "content", "School committee approved the warmer palette after today's walkthrough."
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tasks[0].comments[0].content").value("School committee approved the warmer palette after today's walkthrough."));

        mockMvc.perform(get("/api/community/projects")
                .param("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].tasks[0].comments[0].authorUsername").value("project_coordinator"));

        mockMvc.perform(get("/api/community/proposals/{proposalId}", proposalId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Paint the crossing mural"));

        boardRepository.findById(UUID.fromString(boardId))
            .orElseThrow(() -> new AssertionError("Board should persist"));
    }
}
